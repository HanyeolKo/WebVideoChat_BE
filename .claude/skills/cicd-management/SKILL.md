---
name: cicd-management
description: WebVideoChat 백엔드의 GitHub Actions·Docker·nginx·배포 파이프라인 관리 방법론. 워크플로우(.github/workflows/deploy.yml) 수정, 멀티스테이지 Dockerfile·docker-compose·nginx 변경, GHCR 이미지/시크릿/환경변수 매핑 점검 시 반드시 사용. GitHub Actions는 이 하네스를 통해 관리한다. devops 에이전트가 참조.
---

# CI/CD Management — 백엔드 배포 파이프라인 가이드

WebVideoChat 백엔드의 CI/CD와 인프라 파일을 관리한다. **GitHub Actions는 이 스킬·devops 에이전트를 통해 관리한다.** devops 에이전트가 참조한다.

## 배포 아키텍처 (GHCR + self-hosted 러너)

> 빌드와 배포를 잡으로 분리한다. **build-and-push**는 GitHub 클라우드 러너(`ubuntu-latest`)에서 이미지를 빌드해 GHCR에 push, **deploy**는 배포서버의 **self-hosted 러너**에서 `docker compose pull && up -d`를 수행한다. SSH·webhook·Watchtower 불필요(GitHub이 잡을 서버 러너로 직접 디스패치).

```
push(master) → build-and-push(클라우드): 멀티스테이지 빌드(gradle build 포함) → GHCR :latest
             → deploy(self-hosted): cd $DEPLOY_DIR && docker compose pull && up -d
push(v*)     → build-and-push: GHCR :<tag> 보관 (배포 안 함 / 롤백 대상)
```

### 보안 — public 레포 + self-hosted 러너 (필수)
- deploy 잡은 self-hosted 러너에서 돈다. 포크 PR이 서버에서 임의 코드를 실행하지 못하도록:
  - 워크플로우에 **`pull_request` 트리거를 절대 두지 않는다.**
  - deploy 잡에 **`if`로 "기본 브랜치 push 또는 workflow_dispatch"** 조건을 건다.
- 이 두 가지가 깨지면 public 레포에서 서버가 노출된다. 변경 시 반드시 유지.

## 하네스·문서 변경은 배포하지 않는다 (paths-ignore)
하네스(`.claude/**`)와 문서(`**.md`)는 앱이 아니다 → main에 들어가도 **운영 배포를 트리거하면 안 된다.** 그래서 `deploy.yml`의 `on.push`에 `paths-ignore: ['.claude/**', '**.md']`를 둔다.
- `paths-ignore`는 푸시의 **모든** 변경 파일이 목록에 들어갈 때만 skip → 하네스만 바뀐 푸시=배포 안 함, **앱+하네스 혼합 푸시=정상 배포**.
- `.github/**`(워크플로우 자체)는 일부러 무시 목록에서 뺀다 — 워크플로우 변경은 배포로 검증돼야 하므로.

### 브랜치/하네스 운용 규칙 (확정)
- **하네스 정본은 main에 둔다**(동기 모델). feature 브랜치는 main에서 따므로 항상 최신 하네스를 상속 — 별도 merge 의식 불필요.
- **앱 PR에는 `.claude/**`·`CLAUDE.md`를 포함하지 않는다.** 작업 중 하네스를 만졌다면 앱 커밋에서 제외한다.
- **하네스만 갱신할 때**: main에서 `harness-setting` 임시 브랜치 → 하네스 변경만 커밋 → PR → main 병합(`paths-ignore`로 배포 안 됨) → 브랜치 삭제(ephemeral).
- 이렇게 하면 하네스 변경이 배포를 트리거하는 문제가 트리거 레벨에서 차단되고, 브랜치별 하네스 표류도 없다.

## 현재 파이프라인 (`.github/workflows/deploy.yml`)

- 트리거: `master` push(→`:latest`), `v*` 태그(→`:<tag>`), `workflow_dispatch`.
- `permissions: packages: write` + 빌트인 `GITHUB_TOKEN`으로 GHCR 로그인 → **별도 GHCR 시크릿 불필요.**
- `docker/metadata-action`으로 태그 결정(`type=raw latest enable=is_default_branch`, `type=ref event=tag`), `build-push-action`으로 push.
- 이미지: `ghcr.io/hanyeolko/webvideochat-be`.
- **deploy 잡**: `runs-on: [self-hosted]`, `needs: build-and-push`, `env.DEPLOY_DIR=/home/deploy/webVideoChat/back-end`. GHCR 로그인(GITHUB_TOKEN) → `docker compose pull && up -d`. 서버 compose는 `image: ...:${IMAGE_TAG:-latest}`라 `IMAGE_TAG=<tag>`로 롤백 가능.

## 인프라 파일

- `Dockerfile` — **멀티스테이지**: `eclipse-temurin:21-jdk`로 `./gradlew clean build`(테스트 포함) → `21-jre`에 boot jar 복사. plain jar는 `build.gradle`에서 `tasks.named('jar'){enabled=false}`로 비활성화(단일 jar 매칭).
- `docker-compose.yml` — `app`은 `image: ghcr.io/hanyeolko/webvideochat-be:latest`(서버는 빌드하지 않고 pull) + `nginx`(80/443).
- `nginx/nginx.conf` — REST 프록시 + **WebSocket 업그레이드 헤더**(시그널링 필수). 80→443 리다이렉트, TLS.
- `nginx/certs/` — TLS 인증서(레포에 없을 수 있음, 서버 측).

## 변경 시 점검 항목

1. **단일 jar 보장**: Dockerfile이 `build/libs/*.jar`를 단일 매칭에 의존. `build.gradle`의 plain jar 비활성화를 유지하라. version/artifact명 변경은 영향 없음(와일드카드).
2. **CORS env 매핑 사슬**: `CORS_ALLOWED_ORIGINS`(서버 `.env`) → `application-prod.yml`의 `cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:기본값}` → `CorsConfig` `@Value`. **이 매핑이 끊기면 운영 CORS가 기본값으로 고정돼 실제로 막힌다.** 폴백 기본값을 두어 미설정 시 기동 실패는 방지.
3. **WebSocket 프록시**: nginx 변경 시 `proxy_set_header Upgrade`/`Connection "upgrade"` 유지. 빠지면 시그널링 전체가 죽는다.
4. **이미지명/태그 일관성**: 워크플로우 `IMAGE_NAME` ↔ 서버 compose `image:`. 서버 감지 에이전트는 `:latest`를 watch.
5. **GHCR 인증**: Actions는 `GITHUB_TOKEN`. 서버는 별도 PAT(`read:packages`)로 `docker login ghcr.io`.

## 작업 원칙

- **작게, 한 번에 하나.** YAML 한 줄도 배포를 막는다. 단계 의존성과 들여쓰기 검증.
- **정적 + 로컬 검증.** YAML 파싱, 가능하면 로컬 `docker build`. 실배포(태그/머지)는 사용자가 확인.
- **롤백 절차 동반.** `:latest`는 롤백이 없다 → 롤백은 `:<tag>` 이미지로 서버에서 되돌린다. 변경 전 현재 동작 기록.
- **비밀 노출 금지.** 시크릿은 이름만 참조.
- **부재 파일/서버 설정 임의 생성 금지.** 서버 측(감지 에이전트, 호스트 nginx, 인증서, `.env`)은 레포 밖이다. 필요하면 README/문서로 절차만 제시하고 사용자 확인.

## 서버 준비물 (레포 밖)
- repo Settings → Actions → Runners에 **self-hosted 러너 등록**(repo별 1개), 서비스 상주.
- Docker + compose, 러너 계정 `docker` 그룹.
- `$DEPLOY_DIR`에 compose(`image: ...:${IMAGE_TAG:-latest}`) + nginx.conf + 인증서 + `.env`(`CORS_ALLOWED_ORIGINS`).

## 다음 단계 (미완)
- deploy 잡에 **헬스체크 + 자동 롤백**(실패 시 직전 태그로 복귀) 보강.
- PR/푸시용 **CI 검증 워크플로우**(gradle build+test 별도) — 현재는 이미지 빌드 시 테스트가 돌므로 후순위.

## 검증 명령

```
python -c "import yaml; yaml.safe_load(open('.github/workflows/deploy.yml'))"   # YAML 문법
docker build -t webvideochat-be:test .                                          # 멀티스테이지 빌드(테스트 포함)
```
