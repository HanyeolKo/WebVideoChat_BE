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
- `/home/deploy/webVideoChat/back-end`의 `docker-compose.yml`(이 레포 파일 참고: `app`은 `image: ghcr.io/hanyeolko/webvideochat-be:latest`, `nginx` 포함)과 `nginx/nginx.conf`, TLS 인증서(`nginx/certs/`).
- 서버 `.env`에 `CORS_ALLOWED_ORIGINS=...` 기록.
- 감지 에이전트(Watchtower/webhook) 상주.

## 로컬 개발
```
./gradlew bootRun     # dev 프로파일(localhost:5173 CORS 허용), 8080
./gradlew build       # 컴파일 + 테스트
```

## 현재 한계
- DB·인증 없음, 비밀번호 평문 인메모리 저장 → 재시작 시 방 소실, 다중 인스턴스 불가(향후 MSA 전환 시 해결 과제).
