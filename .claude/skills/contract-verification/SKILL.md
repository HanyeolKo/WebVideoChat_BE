---
name: contract-verification
description: WebVideoChat FE↔BE 경계면(계약) 정합성 검증 방법론. BE의 REST 엔드포인트·WebSocket 경로·응답 DTO가 FE 기대 shape과 일치하는지 교차 비교한다. FE/BE 강제 분리로 생긴 경계 버그를 잡을 때, 계약 변경 영향 평가 시 반드시 사용. contract-qa 에이전트가 참조.
---

# Contract Verification — 경계면 정합성 검증

FE와 BE를 한 프로젝트에서 강제 분리했기 때문에, 둘 사이 계약 불일치가 가장 흔한 버그 원인이다. 이 스킬은 그 경계면을 교차 비교하는 방법을 정의한다.

## 핵심 원칙: 교차 비교

"BE에 엔드포인트가 존재한다"는 검증이 아니다. **BE가 실제로 내놓는 shape**과 **FE가 실제로 기대하는 shape**을 나란히 놓고 1:1로 대조해야 한다. 한쪽만 보면 경계 버그를 놓친다.

현재 계약의 기준선(baseline)은 `references/contract-spec.md`에 정의돼 있다. **이 BE 하네스는 레포 단독으로 완결된다** — FE는 별도 git 레포이며 함께 클론돼 있다는 보장이 없으므로, 검증은 **`contract-spec.md`(기준선) ↔ BE 실제 코드** 대조를 기본으로 한다. 명세와 코드가 다르면 명세가 낡은 것인지 코드가 잘못된 것인지 판단해 보고한다. FE 실코드는 읽지 않는 것을 전제한다.

> **이 레포의 `contract-spec.md`는 상위 관제탑 정본의 거울(mirror)이다.** 두 레포가 함께 클론된 환경(`WebVideoChat/`)에서는 정본 `WebVideoChat/.claude/skills/contract-sync/references/contract-spec.canonical.md`이 단일 진실이고, 이 파일은 그 사본이다. **계약을 바꿀 때는 이 거울만 고치지 말고** 관제탑 `contract-steward`(또는 `contract-sync` 스킬)를 통해 정본 → 두 레포 거울 순으로 동기화한다. 관제탑 없이 레포 단독으로 실행 중이면, 거울을 갱신하되 "정본·FE 거울 동기화 필요"를 보고에 명시한다.

## 검증 절차

1. **기준선 로드**: `references/contract-spec.md`를 읽는다(= FE가 기대하는 shape의 기록).
2. **BE 측 읽기**: `RoomController`(경로/메서드/요청·응답 타입), `dto/`(필드명·타입), `WebSocketConfig`(WS 경로), `SocketHandler`(메시지 중계 형태), `CorsConfig`(오리진).
3. **대조표 작성**: 각 경계면마다 BE 실제 shape ↔ 명세(FE 기대) shape ↔ 일치 여부.
4. **등급 부여 & 보고**. 계약을 깨는 변경이면 `contract-spec.md`를 갱신하고, FE 레포에도 동기화가 필요함을 보고에 명시한다.

## 검증 대상 4개 경계면

### 1. REST 계약
- 경로/메서드 일치: FE `axiosInstance.get/post` 경로 == BE `@RequestMapping`+`@GetMapping/@PostMapping`.
- 요청 body 필드: FE payload 인터페이스 == BE `*Request` DTO 필드명.
- 응답 body 필드: FE 응답 인터페이스 == BE `*Response` DTO 필드명.
- **흔한 버그**: 필드명 케이스 불일치(`roomName` vs `room_name`), 누락 필드, 타입 불일치(string vs number), HTTP 상태코드 미처리(FE가 401/404 핸들링 안 함).

### 2. WebSocket 계약
- 경로: FE `${wsBase}/socket/${roomId}` == BE `/socket/{roomId}`.
- 시그널링 메시지 payload: FE가 `send`하는 객체 구조 == BE `handleTextMessage`가 그대로 중계하는 구조 == 다른 피어 FE의 `onMessage` 파싱 구조. BE는 단순 중계라 구조를 강제하지 않으므로 **FE↔FE 합의가 실질 계약**이다.

### 3. CORS / 오리진
- BE `CorsConfig` 허용 오리진 ⊇ FE 실제 오리진(dev: `localhost:5173`, prod: 배포 도메인).
- `allowCredentials(true)`이면 와일드카드 오리진 불가 — 명시 오리진 필요.

### 4. 환경변수 계약
- FE `VITE_API_BASE_URL`/`VITE_WS_BASE_URL`이 BE 호스트/포트를 가리키는지.
- 배포 시 시크릿 → `.env` → Spring `@Value` 매핑이 끝까지 연결되는지(`cicd-management` 스킬과 교차).

## 등급

- **BLOCKER**: 런타임에 반드시 깨진다(경로 불일치, 필수 필드 누락/오타).
- **WARN**: 상황에 따라 깨진다(상태코드 미처리, 오리진 누락).
- **INFO**: 잠재 위험(향후 MSA 분리 시 깨질 결합).

## 출력 형식

각 불일치는 **양쪽 위치(파일:라인)** + **양쪽 shape** + **수정 방향**을 포함한다. 검증자는 수정하지 않고 보고만 한다(수정은 engineer/FE에 위임). 검증 불가 구간은 명시한다.

> 계약이 바뀌면 `references/contract-spec.md`를 갱신하는 것을 잊지 마라. 명세가 항상 최신 기준선이어야 한다.
