# WebVideoChat_BE

Spring Boot 3.2 / Java 21 화상채팅 백엔드. REST(`/api/rooms`) + WebRTC 시그널링(WebSocket `/socket/{roomId}`). 방 상태는 인메모리.

## 기능
- 채팅방 생성/목록/입장 (목록·입장 시 비밀번호 노출 방지)
- WebRTC 시그널링 중계 (같은 방의 다른 피어로 offer/answer/candidate 전달)
- HTTPS/TLS, WebSocket 프록시 (nginx)

## 배포 구조 (GHCR + 온프레미스 자동 트리거)

```
git push (master) ──▶ GitHub Actions ──▶ GHCR(:latest) ──▶ [서버 감지 에이전트] ──▶ pull & 재기동
git push (v*)     ──▶ GitHub Actions ──▶ GHCR(:<tag>)  (버전 스냅샷 / 롤백 대상)
```

- **GitHub Actions**(`.github/workflows/deploy.yml`)는 멀티스테이지 `Dockerfile`로 이미지를 빌드(이 과정에서 `./gradlew clean build` 테스트 포함)해 GHCR(`ghcr.io/hanyeolko/webvideochat-be`)에 push까지만 한다. 서버에 SSH로 접속하지 않는다.
- GHCR 로그인은 빌트인 `GITHUB_TOKEN`(+ `permissions: packages: write`)을 사용하므로 **별도 시크릿이 필요 없다.**
- 서버 배포는 온프레미스 서버의 **감지 에이전트(Watchtower 또는 자체 webhook 수신기)**가 새 `:latest`를 감지해 `docker compose pull && up -d`로 수행한다. (서버 설정, 이 레포 밖)

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
