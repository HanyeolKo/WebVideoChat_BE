# WebVideoChat_BE

Spring Boot 3.2 / Java 21 화상채팅 백엔드. REST(`/api/rooms`) + WebRTC 시그널링(WebSocket `/socket/{roomId}`). 방 상태는 인메모리.

## 기능
- 채팅방 생성/목록/입장 (목록·입장 시 비밀번호 노출 방지)
- WebRTC 시그널링 중계 (같은 방의 다른 피어로 offer/answer/candidate 전달)
- HTTPS/TLS, WebSocket 프록시 (nginx)

## 배포 구조 (GHCR + self-hosted 러너)

```
push(master) ─▶ build-and-push 잡(GitHub 클라우드 러너): 빌드 → GHCR :latest
                         │ needs
                         ▼
                deploy 잡(배포서버의 self-hosted 러너): docker compose pull && up -d
push(v*)     ─▶ build-and-push 잡: GHCR :<tag> 보관 (배포는 안 함 / 롤백 대상)
```

- **build-and-push**(클라우드 러너): 멀티스테이지 `Dockerfile`로 이미지를 빌드(`./gradlew clean build` 테스트 포함)해 GHCR(`ghcr.io/hanyeolko/webvideochat-be`)에 push.
- **deploy**(배포서버의 self-hosted 러너): GHCR에서 pull 후 `DEPLOY_DIR`(`/home/deploy/webVideoChat/back-end`)에서 `docker compose pull && up -d`. **SSH·webhook·Watchtower 불필요** — GitHub이 잡을 서버 러너로 직접 내려보낸다.
- GHCR 로그인은 빌트인 `GITHUB_TOKEN`(+ `packages: write`)을 사용하므로 **별도 시크릿/계정이 필요 없다.**
- 롤백: 서버 `DEPLOY_DIR`에서 `IMAGE_TAG=<이전태그> docker compose up -d` (compose가 `:${IMAGE_TAG:-latest}` 참조).

### self-hosted 러너 설치 (배포서버, 1회)
1. repo → Settings → Actions → Runners → **New self-hosted runner** 안내대로 다운로드·`config`(등록 토큰).
2. 서비스로 상주: `sudo ./svc.sh install && sudo ./svc.sh start`.
3. 서버 요건: Docker + compose 플러그인, 러너 계정을 `docker` 그룹에 추가(`sudo usermod -aG docker <runner-user>`).
4. `DEPLOY_DIR`에 `docker-compose.yml`(이 레포 파일 참고: `app`은 `image: ghcr.io/...:${IMAGE_TAG:-latest}`), `nginx/nginx.conf`, TLS 인증서, `.env`(`CORS_ALLOWED_ORIGINS=...`) 배치.

### ⚠️ public 레포 보안 (필수 하드닝)
- 이 워크플로우는 `pull_request`에서 트리거되지 않으며, deploy 잡은 기본 브랜치 push/수동 실행에서만 동작한다(포크 PR이 self-hosted 러너를 못 건드림).
- 추가로 repo Settings → Actions → General에서: **Fork pull request workflows는 승인 필요**로, **Workflow permissions/실행 승인**을 외부 기여자에 대해 제한할 것.

## 환경변수
- `CORS_ALLOWED_ORIGINS` — 운영 CORS 허용 오리진(콤마 구분). 서버 `.env`로 주입 → `application-prod.yml`의 `cors.allowed-origins`에 매핑. 미설정 시 `http://localhost:5173`로 폴백.

## 서버 준비물 (레포 밖)
- 서버에 GHCR **read 권한 PAT**(`read:packages`)로 `docker login ghcr.io`.
- `/home/deploy/webVideoChat/back-end`의 `docker-compose.yml`(이 레포 파일 참고: `app`은 `image: ghcr.io/hanyeolko/webvideochat-be:${IMAGE_TAG:-latest}`, `nginx` 포함)과 `nginx/nginx.conf`, TLS 인증서(`nginx/certs/`).
- 서버 `.env`에 `CORS_ALLOWED_ORIGINS=...` 기록.
- GHCR read 권한 PAT로 `docker login ghcr.io`. (self-hosted 러너가 push 후 직접 pull&up 하므로 Watchtower/webhook은 불필요.)

## 로컬 개발
```
./gradlew bootRun     # dev 프로파일(localhost:5173 CORS 허용), 8080
./gradlew build       # 컴파일 + 테스트
```

## 현재 한계
- DB·인증 없음, 비밀번호 평문 인메모리 저장 → 재시작 시 방 소실, 다중 인스턴스 불가(향후 MSA 전환 시 해결 과제).

---

## AI 주도개발 하네스 (Claude Code)

이 레포는 Claude Code가 백엔드를 **안전하고 점진적으로** 진화시키도록 전문 에이전트 팀 하네스를 갖췄다. 하네스는 `harness-setting` 브랜치에서 버전 관리되며, `.claude/agents/`(누가)와 `.claude/skills/`(어떻게)로 구성된다.

### 구성

| 구분 | 이름 | 역할 |
|------|------|------|
| 오케스트레이터 | `be-harness` (스킬) | 요청을 분류해 필요한 에이전트만 팀으로 투입·조율 |
| 에이전트 | `be-architect` | 점진적 리팩토링 계획, 결합도 분석, MSA를 향한 "다음 한 스텝" 설계 |
| 에이전트 | `be-engineer` | Spring Boot 코드 구현·수정 (`spring-msa-dev` 스킬 참조) |
| 에이전트 | `contract-qa` | FE↔BE 경계면 정합성 검증 (`contract-verification` 스킬 참조) |
| 에이전트 | `devops` | GitHub Actions·Docker·nginx·배포 (`cicd-management` 스킬 참조) |
| 스킬 | `spring-msa-dev` | 백엔드 코드 컨벤션 + 점진적 리팩토링 원칙 |
| 스킬 | `contract-verification` | 경계면 교차 비교 방법론 (계약 명세 거울 보유) |
| 스킬 | `cicd-management` | GHCR + self-hosted 러너 배포 파이프라인 관리 |

**실행 모드: 에이전트 팀.** 모든 에이전트는 `model: opus`. 산출물은 `_workspace/`에 단계별로 보존(감사·롤백).

### 핵심 철학 — 점진적 + 되돌리기 쉽게

원래 단일 프로젝트였고 MSA 전환을 위해 FE/BE를 강제 분리한 과도기다. **지금 당장 MSA로 쪼개지 않는다.** 모든 작업을 독립 검증·롤백 가능한 최소 스텝으로 쪼개고, "내가 깬 것"과 "원래 깨진 것"을 구분한다. (`claude/msa-architecture-migration` 브랜치의 빅뱅 분리는 *방향*일 뿐 다음 스텝이 아니다.)

### 상위 통합 관제 하네스 연동 (선택적)

> **이 레포 하네스는 독립적으로 완전히 동작한다.** 이 레포만 클론해서 `be-harness`로 기능·리팩토링·계약 검증·배포를 모두 처리할 수 있다. 아래의 관제탑은 **선택적 상위 층**일 뿐이며, 없어도 하네스는 그대로 쓸 수 있다(관련 포인터는 모두 "관제탑이 있으면 위임, 없으면 자체 처리·보고"로 분기한다).

두 레포가 함께 클론된 환경(`WebVideoChat/`)에서는 그 위에 **통합 관제 하네스**(관제탑)가 있어, 계약 변경·교차 레포 작업을 조율한다:

- **계약(REST·WS·CORS·env)을 바꾸는 변경**은 단일 레포로 끝나지 않는다. 관제탑의 `contract-steward`가 **계약 정본**(`WebVideoChat/.claude/skills/contract-sync/references/contract-spec.canonical.md`)을 갱신하고, 이 레포의 `contract-spec.md`(거울)와 FE 거울을 동기화한다. 즉 이 레포의 계약 명세는 **정본의 거울**이다.
- **FE와 동시/순서 배포**가 필요하면 관제탑의 `release-coordinator`가 순서(보통 BE 먼저 → FE)와 롤백을 조율한다.
- 관제탑 없이 이 레포만 단독으로 작업할 때는, 계약 변경 시 "FE 측 동반 변경 + 양쪽 contract-spec 동기화 필요"를 명시 보고한다.

### 사용법

작업 디렉토리에서 Claude Code를 실행하고 자연어로 요청하면 트리거된다.
- "방 입장 응답에 필드 추가" → be-harness가 architect→engineer→contract-qa로 처리(계약 영향 시 관제탑 에스컬레이션).
- "배포 워크플로우 수정" → devops가 `cicd-management` 기준으로 처리.
- "FE랑 정합성 점검" → contract-qa가 거울 ↔ BE 실코드 대조.
