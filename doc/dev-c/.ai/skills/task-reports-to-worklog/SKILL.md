---
name: task-reports-to-worklog
description: task 폴더별로 누적된 report 문서를 읽어 하나의 누적 worklog 문서로 합치는 스킬이다. 사용자가 `doc/dev-c/.person/reports/task-{번호}/` 안의 여러 report를 바탕으로 `doc/dev-c/worklog/worklog-{번호}.md`를 작성하거나 갱신해달라고 요청할 때 사용한다. 예: "task-017 리포트들 읽고 worklog 만들어줘", "report 모아서 worklog-017 써줘", "task 폴더 report 기반으로 작업 로그 갱신해줘".
---

# Task Report 기반 Worklog 작성 스킬

## 개요

같은 task 번호 아래에 쌓여 있는 여러 report 파일을 읽고, 중복을 제거하면서 하나의 최신 누적 작업 로그로 정리한다.
출력 파일은 항상 `doc/dev-c/worklog/worklog-{번호}.md`이며, report는 여러 개일 수 있지만 worklog는 task 번호당 1개만 유지한다.

## 입력 해석 규칙

1. 먼저 대상 task 번호를 결정한다.
   - 사용자가 `task-017`, `worklog-017`, `17번 task`처럼 번호를 명시하면 그 번호를 그대로 사용한다.
   - 사용자가 `doc/dev-c/.person/reports/task-017/` 같은 경로를 주면 폴더명에서 번호를 추출한다.
   - 사용자가 세부 step만 말하더라도 상위 task 번호가 분명하면 그 번호를 사용한다.
2. 입력 report 폴더는 기본적으로 `doc/dev-c/.person/reports/task-{번호}/`로 본다.
3. 보조 문서가 있으면 함께 읽는다.
   - `doc/dev-c/task/task-{번호}.md`
   - `doc/dev-c/workflow/workflow-{번호}.md`
4. report 폴더가 비어 있거나 존재하지 않으면 없는 내용을 추측하지 말고, report가 없어서 worklog를 만들 수 없다고 분명하게 말한다.

## 작업 절차

1. `task-{번호}` 폴더의 report 파일 목록을 읽는다.
2. 파일명을 기준으로 오래된 report부터 최신 report 순서로 정렬한다.
3. 각 report에서 다음 정보를 추출한다.
   - 작업명
   - 작업 일시
   - 진행 단계
   - 전체 작업 흐름
   - 핵심 코드에서 드러나는 실제 변경 요지
   - 기술 딥다이브에서 드러나는 핵심 기술 포인트
   - 세션 중 확인 가능한 검증 결과, 변경 파일, 남은 리스크
4. 필요하면 `task-{번호}.md`와 `workflow-{번호}.md`를 함께 읽어 작업 목표와 수용 기준을 보강한다.
5. 여러 report를 하나의 누적 worklog로 합친다.
6. `doc/dev-c/worklog/worklog-{번호}.md`에 저장한다.
7. 저장 후 어떤 report들을 기준으로 worklog를 갱신했는지 짧게 알려준다.

## 누적 합성 규칙

- worklog는 report 여러 개를 그대로 이어붙인 문서가 아니라, 누적 요약본이어야 한다.
- 같은 파일이 여러 report에 반복 등장하면 `변경 파일`에는 한 번만 적는다.
- 같은 구현 항목이 여러 번 언급되면 가장 최신 상태를 기준으로 정리한다.
- 보고서 사이에 정보가 충돌하면 최신 report를 우선하고, 모순이 중요한 경우 `남은 TODO / 리스크`에 남긴다.
- 검증 결과는 최신 성공 여부를 우선 보여주되, 중요한 실패 이력이나 환경 이슈는 함께 남긴다.
- 세부 step 단위 report가 여러 개 있으면 step별 결과를 묶어 더 큰 구현 흐름으로 다시 정리한다.
- 작업명이 조금씩 달라도 동일 task 폴더에 있으면 같은 작업의 하위 이력으로 취급한다.

## 저장 규칙

- 출력 파일은 항상 `doc/dev-c/worklog/worklog-{번호}.md`이다.
- 같은 task 번호에 대해 worklog 파일은 하나만 유지한다.
- 새 report가 추가되면 기존 worklog를 덮어써 최신 누적본으로 갱신한다.
- 날짜별 새 worklog 파일을 추가로 만들지 않는다.
- worklog는 report 폴더를 요약한 공식 누적 문서로 본다.

## 권장 출력 형식

아래 형식을 기본으로 사용한다.

````markdown
# task-017 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-017.md` 확인
- [x] report 폴더 기준 누적 요약 작성

## 작업 목표
- ...

## 보고서 소스
- `doc/dev-c/.person/reports/task-017/report-20260318-0900.md`
- `doc/dev-c/.person/reports/task-017/report-20260318-1130-task-17-2.md`

## 변경 파일
- `...`

## 구현 내용
1. ...
2. ...

## 검증 결과
- 실행 명령어: `...`
- 결과: `...`

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-017.md`
- 로컬: `doc/dev-c/workflow/workflow-017.md`

## 남은 TODO / 리스크
- ...
````

기존 `worklog-{번호}.md`가 이미 있으면 헤더 스타일은 최대한 유지하되, 내용은 최신 누적 상태로 갱신한다.
새로 만들 때는 `# task-017 작업 로그` 형식을 기본으로 사용한다.

## 섹션 작성 규칙

- `작업 목표`는 `task-{번호}.md`의 목적과 report의 반복 공통 목표를 합쳐 작성한다.
- `보고서 소스`에는 이번 worklog 작성에 읽은 report 파일 경로를 모두 남긴다.
- `변경 파일`은 모든 report의 파일 목록을 합친 뒤 중복 제거해 작성한다.
- `구현 내용`은 시간순 나열보다 기능 흐름 중심으로 다시 정리한다.
- `검증 결과`는 가장 최근 검증 결과를 우선하고, 실패 이력은 중요할 때만 덧붙인다.
- `남은 TODO / 리스크`는 report들에 남은 미해결 사항만 모아 중복 제거 후 작성한다.

## 품질 기준

- report에 없는 내용을 임의로 만들어 넣지 않는다.
- 같은 의미의 중복 문장을 그대로 반복하지 않는다.
- 구현 내용은 "무엇을 왜 바꿨는지"가 보이도록 재서술한다.
- 한 번에 읽었을 때 task 전체 진행 상황이 보이도록 정리한다.
- worklog는 회고 메모가 아니라 작업 기록 문서이므로, 변경 파일과 검증 결과를 반드시 포함한다.
