---
name: pr-merge-flow
description: WebVideoChat 백엔드의 PR 생성→검수→수정→병합을 자동화하는 오케스트레이터. "PR 생성", "PR 올려", "풀리퀘스트", "병합", "merge", "PR 리뷰받고 합쳐줘", "리뷰 후 병합" 요청 시 반드시 이 스킬을 사용. gh CLI로 PR을 만들고, pr-reviewer(opus) 에이전트가 적대적 검수하며, blocker를 be-engineer가 수정(최대 2라운드)하고, 조건부 자동병합(blocker 0 + CI green)한다. 교차 레포 PR은 관제탑(release-coordinator)이 조율하므로 단일 BE 레포 PR일 때 사용.
---

# PR Merge Flow — PR 생성·검수·병합 자동화 (BE)

PR을 만들고, 최고 수준 모델로 검수하고, 지적을 고쳐, 안전 조건이 충족될 때만 병합한다. **실행 모드: 생성–검증 + 조건부 게이트.**

> **왜 게이트가 필요한가.** 이 레포는 `main` 병합 시 self-hosted 러너가 **운영 자동배포**한다(`.github/workflows/deploy.yml`). 병합 = 되돌리기 어려운 운영 배포다. 검수·수정은 자동으로 끝까지 돌리되, **병합만큼은 안전 조건**(blocker 0 + CI green)을 통과할 때만 자동 수행하고, 아니면 멈추고 사용자에게 넘긴다.

## 안전 게이트 (확정 정책)
- **자동 병합 조건**: pr-reviewer `verdict: PASS`(blocker 0건) **그리고** `gh pr checks` 전부 green. 둘 다여야 자동 병합.
- **수정 루프 상한**: 리뷰→수정→재리뷰 **최대 2라운드**. 이후에도 blocker 잔존 시 병합하지 않고 사용자 핸드오프.
- **계약 위반(contract_touched: yes)**: 즉시 중단하고 관제탑(`contract-steward`/`release-coordinator`)으로 에스컬레이션. 자동 병합 금지.
- 사용자가 특정 실행에서 다른 수위를 지시하면 그 지시가 기본 정책을 덮어쓴다.

## 팀 / 자원
| 자원 | 역할 |
|------|------|
| `pr-reviewer` (opus) | PR diff·CI 적대적 검수, blocker/suggestion 등급화 판정 |
| `be-engineer` (opus) | blocker 수정 구현 |
| gh CLI | PR 생성·체크 조회·병합 |

## Phase 0: 컨텍스트 확인
- `_workspace/` 존재 + "그 PR 이어서/재리뷰" → 기존 verdict 라운드 이어서.
- 새 PR 요청 → 기존 `_workspace/`를 `_workspace_prev/`로 이동 후 새 실행.
- 선행: `gh auth status`로 인증 확인. 실패면 `! gh auth login` 안내.

## Phase 1: PR 생성
1. **브랜치 정리**: 변경이 `main` 작업트리에 있으면 feature 브랜치로 — `git switch -c feat/<요약>`, 관련 파일만 스테이징. 운영배포 트리거인 `main`에 직접 커밋·푸시 금지.
2. **커밋**: 의미 단위. 메시지 말미 `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.
3. **푸시 & PR 생성**: `git push -u origin <branch>` → `gh pr create --base main --title "<제목>" --body "<요약 + 변경 파일 + 빌드 결과 + 🤖 Generated with Claude Code>"`.
4. PR 번호를 `_workspace/pr_meta.md`에 기록.
5. PR 생성 직전 한 줄로 무엇을 올리는지 보고. 첫 원격 브랜치/PR이면 사용자에게 알린다.

## Phase 2: 검수 → 수정 루프 (최대 2라운드)
각 라운드:
0. **보안 전용 패스**: `/security-review` 스킬을 이 PR의 변경(현재 브랜치 pending diff)에 돌려 보안 발견을 수집한다 → `_workspace/{round}_security-review.md`.
1. `pr-reviewer`(opus)로 PR 검수하되 **0의 security-review 결과를 입력으로 함께 전달**한다. 일반 검토 + 보안 발견 등급화를 합쳐 판정 → `_workspace/{round}_pr-reviewer_verdict.md`.
2. 판정 분기:
   - `contract_touched: yes` → **루프 중단**, 관제탑 에스컬레이션(Phase 4 계약 경로).
   - `verdict: PASS` + CI green → Phase 3.
   - `verdict: PASS` + CI 미green → 병합 보류, CI 보고(Phase 4).
   - `CHANGES_REQUESTED` → blocker를 `be-engineer`에 전달해 수정 → `./gradlew build` 통과 확인 → 커밋·푸시(PR 자동 갱신) → 다음 라운드.
3. 2라운드 후에도 blocker 잔존 → 루프 종료, Phase 4.

> suggestion은 병합 비차단. 자동 병합 조건 판단에 미포함.

## Phase 3: 조건부 자동 병합
안전 게이트(blocker 0 + CI green) 통과 시에만:
1. 최종 CI 재확인: `gh pr checks <num>` 전부 green.
2. 병합: `gh pr merge <num> --squash --delete-branch`.
3. **병합 = 운영 배포 트리거**임을 보고에 명시, deploy.yml 진행 안내. 가능하면 `gh run watch`로 배포 잡 1회 확인.
4. `_workspace/merge_report.md`에 기록.

## Phase 4: 종합 및 보고
- 생성 PR(URL), 라운드별 blocker/해소, CI 상태, 병합 여부·이유 보고.
- **병합 안 한 경우**: 무엇이 막았는지, 남은 blocker·각 fix 지시, 다음 행동(사용자 검토 후 `gh pr merge`, 또는 관제탑 경로) 명시.
- **계약 위반**: 관제탑 `contract-steward`(정본)+`release-coordinator`(FE 동반·배포 순서) 경로. 관제탑 부재 시 "FE 동반 변경 + 양쪽 contract-spec 동기화 필요" 명시.

## 에러 핸들링
- **gh 인증/권한 실패**: 중단, `gh auth login` 안내. 우회 금지.
- **푸시 충돌**: 강제 푸시 금지. 상태 보고 후 지시 대기.
- **CI 영구 실패(내 변경 무관)**: 원래 깨진 것인지 구분 보고, 자동 병합 보류.
- **리뷰어/엔지니어 1회 실패**: 1회 재시도, 재실패 시 해당 라운드 결과 없이 보고.

## 테스트 시나리오
**정상 흐름:** "이 리팩토링 PR 올리고 리뷰받고 합쳐줘"
→ Phase 0(인증 OK) → Phase 1(feat 브랜치·`gh pr create`) → R1: pr-reviewer가 세션 맵 동시성 blocker → be-engineer 수정·`./gradlew build`·푸시 → R2: PASS + CI green → Phase 3 squash 병합 → "운영 배포 트리거됨" 보고.

**게이트 정지 흐름:** R1·R2 모두 blocker 잔존(또는 CI red)
→ 자동 병합 안 함 → 남은 blocker·fix·CI 로그 위치 보고, 사용자 핸드오프.

**계약 위반 흐름:** PR이 응답 DTO shape을 바꿈
→ 루프 즉시 중단 → 관제탑 경로로 에스컬레이션, FE 동반·배포 순서 조율 안내.

## 후속 작업
"그 PR 다시 리뷰", "수정 반영하고 다시", "이제 병합해" 후속도 처리. Phase 0에서 `_workspace/` verdict 라운드 확인 후 진행.
