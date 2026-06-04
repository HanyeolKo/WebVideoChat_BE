# WebVideoChat 백엔드

원래 단일 프로젝트였으나 MSA 전환을 위해 FE/BE로 분리한 **과도기** 상태. 스택: Spring Boot 3.2 / Java 21 / Gradle, REST + WebRTC 시그널링(WebSocket), 인메모리 상태.

> **작업 철학 — 점진적 + 되돌리기 쉽게.** 지금 당장 MSA로 쪼개지 않는다. 한 번에 크게 바꾸지 않고, 독립 검증·롤백 가능한 최소 스텝으로 진행한다. 소스가 불완전할 수 있음을 전제하고, "내가 깬 것"과 "원래 깨진 것"을 구분한다.

## 하네스: 백엔드 작업 자동화

**목표:** Spring Boot 백엔드의 기능·리팩토링·계약 검증·배포를 전문 에이전트 팀으로 안전하고 점진적으로 수행한다.

**트리거:** BE 코드 구현/수정, 점진적 리팩토링, FE↔BE 계약 정합성 점검, GitHub Actions·Docker·배포 변경 요청 시 `be-harness` 스킬을 사용하라. 단순 질문(코드 위치 등)은 직접 응답 가능.

**구성:** 에이전트 팀 모드. 에이전트는 `.claude/agents/`, 스킬은 `.claude/skills/`에서 관리(상세는 거기서 확인).

**변경 이력:**
| 날짜 | 변경 내용 | 대상 | 사유 |
|------|----------|------|------|
| 2026-06-04 | 초기 구성 (be-architect/be-engineer/contract-qa/devops + be-harness 오케스트레이터 + spring-msa-dev/contract-verification/cicd-management 스킬) | 전체 | FE/BE 강제 분리 과도기 지원, 점진적 리팩토링·계약 정합성·CI/CD 관리 |
