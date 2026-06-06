---
name: spring-msa-dev
description: WebVideoChat 백엔드(Spring Boot 3.2 / Java 21 / Gradle) 코드를 작성·수정할 때의 컨벤션과 점진적 리팩토링 원칙. 컨트롤러·서비스·WebSocket·DTO·설정 작성, MSA를 향한 단계적 구조 개선 시 사용. BE 코드를 만지는 모든 작업에서 참조.
---

# Spring MSA Development — 백엔드 구현 가이드

WebVideoChat 백엔드 코드를 작성·수정할 때 따르는 컨벤션이다. be-engineer와 be-architect가 참조한다.

## 스택 & 구조

- Spring Boot 3.2.0, Java 21, Gradle, Lombok.
- 패키지 루트: `com.khy.webvideochat`.
- 레이어: `controller/` (REST) → `service/` (비즈니스 + 인메모리 상태) → `dto/` (`request/`, `response/`). `websocket/` (시그널링), `config/` (CORS 등).

## 현재 아키텍처의 사실

- **상태는 인메모리.** `ChatRoomManageService`가 `Map<String, RoomDTO>`로 방을 보관한다. DB 없음. 따라서 서버 재시작 시 방이 사라지고, **수평 확장이 불가능**하다(MSA 분리 시 첫 번째 해결 과제).
- **REST와 WebSocket이 같은 인메모리 상태를 공유**한다. `SocketHandler`는 `ChatRoomManageService`를 통해 방 세션 목록(`chatUsers`)에 접근한다. 이 공유가 두 도메인을 결합시킨다.
- WebSocket roomId는 핸드쉐이크 인터셉터가 URL에서 추출해 `ThreadLocal`로 전달한다.

## 점진적 리팩토링 원칙

> 한 번에 크게 바꾸지 않는다. 오류 시 되돌리기 어렵기 때문이다.

1. **한 스텝 = 빌드 통과 + 동작 유지 + 롤백 가능.** 각 변경 후 `./gradlew build`로 컴파일·테스트 확인.
2. **계약을 깨는 변경은 격리하라.** 엔드포인트/DTO 변경은 FE에 영향을 준다. 가능하면 기존 계약을 유지한 채 내부만 바꾸고, 불가피하면 명시적으로 표시한다.
3. **MSA를 향한 안전한 순서**(지금 당장이 아니라 방향):
   - (a) 인메모리 상태를 인터페이스 뒤로 추상화 → (b) 외부 저장소(Redis 등)로 이전 → (c) Room/Signaling 도메인 분리 → (d) 독립 서비스 추출. 각 단계는 그 자체로 독립 배포 가능해야 한다.
4. **추측 금지.** 동시성(`CopyOnWriteArrayList`, `ThreadLocal`) 동작이 미묘하다. 바꾸기 전에 읽고 이해한다.

## 코드 컨벤션

- **파일 헤더 주석**: 기존 파일과 동일한 Javadoc 헤더 양식을 유지한다(`@fileName`, `@author`, `@date`, `@description`, History 표). 새 파일도 이 양식을 따른다.
- **DTO 명명**: 요청은 `*Request`, 응답은 `*Response`(`dto/request`, `dto/response`). Lombok `@Data`/`@AllArgsConstructor` 사용.
- **컨트롤러**: `@RestController` + `@RequiredArgsConstructor`, 생성자 주입. `ResponseEntity`로 상태코드 명시.
- **들여쓰기**: 기존 파일은 2-space. 주변 파일과 동일하게 맞춘다.

## 빌드 & 검증

```
./gradlew build          # 컴파일 + 테스트
./gradlew bootRun        # 로컬 실행 (dev 프로파일 기본)
```

- 프로파일: `application.yml`이 `dev` 활성화. dev는 `localhost:5173` CORS 허용, prod는 `CORS_ALLOWED_ORIGINS` 환경변수.
- 빌드가 원래 깨져 있을 수 있다. 변경 전 한 번 빌드해 baseline을 잡고, 변경 후 비교해 회귀를 구분한다.

## 하지 말 것

- 인메모리 상태를 영향 분석 없이 DB/캐시로 한 번에 교체 — 큰 변경은 architect의 스텝 분할을 거친다.
- FE 계약(경로·DTO 필드명) 변경을 contract-qa 검증 없이 머지.
- 시크릿·실서버 값을 코드에 하드코딩.
