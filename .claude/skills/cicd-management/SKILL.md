---
name: cicd-management
description: WebVideoChat 백엔드의 GitHub Actions·Docker·nginx·배포 파이프라인 관리 방법론. 워크플로우(.github/workflows/deploy.yml) 수정, 멀티스테이지 Dockerfile·docker-compose·nginx 변경, GHCR 이미지/시크릿/환경변수 매핑 점검 시 반드시 사용. GitHub Actions는 이 하네스를 통해 관리한다. devops 에이전트가 참조.
---

# CI/CD Management — 백엔드 배포 파이프라인 가이드

WebVideoChat 백엔드의 CI/CD와 인프라 파일을 관리한다. **GitHub Actions는 이 스킬·devops 에이전트를 통해 관리한다.** devops 에이전트가 참조한다.

## 배포 아키텍처 (GHCR + 온프레미스 자동 트리거)

> **GitHub Actions의 책임은 "이미지 빌드 → GHCR push"까지다.** 서버에 SSH로 접속하지 않는다. 서버 배포는 온프레미스 서버의 감지 에이전트(Watchtower 또는 자체 webhook 수신기)가 새 `:latest`를 감지해 `docker compose pull && up -d`로 수행한다(서버 설정, 레포 밖).

```
push(master) → Actions: 멀티스테이지 빌드(gradle build 포함) → GHCR :latest → [서버 감지] → pull&재기동
push(v*)     → Actions: 빌드 → GHCR :<tag>  (버전 스냅샷 / 롤백 대상)
```

## 현재 파이프라인 (`.github/workflows/deploy.yml`)

- 트리거: `master` push(→`:latest`), `v*` 태그(→`:<tag>`), `workflow_dispatch`.
- `permissions: packages: write` + 빌트인 `GITHUB_TOKEN`으로 GHCR 로그인 → **별도 GHCR 시크릿 불필요.**
- `docker/metadata-action`으로 태그 결정(`type=raw latest enable=is_default_branch`, `type=ref event=tag`), `build-push-action`으로 push.
- 이미지: `ghcr.io/hanyeolko/webvideochat-be`.

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

## 다음 단계 (미완)

- 서버 측 **webhook 수신기 + rollback 지원 `deploy.sh`** 구성(롤백 유지 목적). 서버 URL/시크릿/롤백 정책 확정 후 진행.
- PR/푸시용 **CI 검증 워크플로우**(gradle build+test 별도) 신설 — 현재는 이미지 빌드 시 테스트가 돌므로 후순위.

## 검증 명령

```
python -c "import yaml; yaml.safe_load(open('.github/workflows/deploy.yml'))"   # YAML 문법
docker build -t webvideochat-be:test .                                          # 멀티스테이지 빌드(테스트 포함)
```
