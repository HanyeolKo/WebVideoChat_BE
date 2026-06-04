---
name: cicd-management
description: WebVideoChat 백엔드의 GitHub Actions·Docker·nginx·배포 파이프라인 관리 방법론. 워크플로우(.github/workflows/deploy.yml) 수정, Dockerfile·docker-compose·nginx 변경, 시크릿/환경변수 매핑 점검 시 반드시 사용. GitHub Actions는 이 하네스를 통해 관리한다. devops 에이전트가 참조.
---

# CI/CD Management — 백엔드 배포 파이프라인 가이드

WebVideoChat 백엔드의 CI/CD와 인프라 파일을 관리한다. **GitHub Actions는 이 스킬·devops 에이전트를 통해 관리한다.** devops 에이전트가 참조한다.

## 현재 파이프라인 (`.github/workflows/deploy.yml`)

트리거: `v*` 태그 push 또는 `workflow_dispatch`.

```
build job:
  checkout → JDK 21(temurin) → Gradle 캐시 → chmod +x gradlew
  → ./gradlew clean build (테스트 포함)
  → WebVideoChat-*.jar (plain 제외) 찾아 app.jar로 복사
  → app.jar 아티팩트 업로드
deploy job (needs: build):
  checkout → app.jar 다운로드
  → SCP로 app.jar/Dockerfile/docker-compose.yml/nginx.conf를 서버 전송
  → SSH로: .env에 CORS_ALLOWED_ORIGINS 기록 → docker compose down/build/up
```

시크릿: `SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`, `SSH_PORT`, `CORS_ALLOWED_ORIGINS`.

## 인프라 파일

- `Dockerfile` — JAR 실행 이미지.
- `docker-compose.yml` — `app`(8080 expose) + `nginx`(80/443, 리버스 프록시). `webnet` 브리지.
- `nginx/nginx.conf` — REST 프록시 + **WebSocket 업그레이드 헤더**(시그널링이 동작하려면 `Upgrade`/`Connection` 헤더 전달 필수).
- `nginx/certs/` — TLS 인증서(레포에 없을 수 있음, 서버 측 존재).

## 변경 시 점검 항목

1. **JAR 이름 의존성**: 워크플로우가 `WebVideoChat-*.jar`(plain 제외)에 의존. `build.gradle`의 `version`/artifact명을 바꾸면 `find` 패턴도 함께 갱신.
2. **시크릿→.env→프로퍼티 매핑 추적**: `CORS_ALLOWED_ORIGINS`(시크릿) → 서버 `.env` → `cors.allowed-origins`(Spring `@Value`). 이 사슬이 `application-prod.yml`에서 `cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}`로 연결돼야 실제 적용된다. **매핑 누락은 강제 분리에서 가장 흔한 배포 버그.**
3. **WebSocket 프록시**: nginx 변경 시 `proxy_set_header Upgrade`/`Connection "upgrade"`가 유지되는지 확인. 빠지면 시그널링 전체가 죽는다.
4. **포트 일관성**: compose `expose 8080` ↔ Spring 포트 ↔ nginx upstream.

## 작업 원칙

- **작게, 한 번에 하나.** YAML 한 줄도 배포를 막을 수 있다. 단계 의존성(`needs`)과 들여쓰기를 검증한다.
- **정적 검증까지만.** 워크플로우 실제 실행은 로컬에서 완전 검증 불가. YAML 파싱·단계 점검 후, 실배포는 사용자가 태그 push로 확인하도록 안내한다.
- **롤백 절차 동반.** 변경 전 현재 동작을 기록하고, 실패 시 복구 방법을 제시한다.
- **비밀 노출 금지.** 시크릿 값을 로그/보고서에 쓰지 않는다. 이름만 참조.
- **부재 파일 임의 생성 금지.** `docker-compose.yml`/인증서가 레포에 없으면 "서버 측 존재 추정"으로 표시하고 사용자에게 확인.

## 검증 명령

```
# YAML 문법 점검 (Python 가용 시)
python -c "import yaml,sys; yaml.safe_load(open('.github/workflows/deploy.yml'))"
# Gradle 빌드 산출물 이름 확인
./gradlew build && ls build/libs
```
