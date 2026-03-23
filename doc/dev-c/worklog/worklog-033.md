# task-033 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-033.md` 확인
- [x] `doc/dev-c/workflow/workflow-033.md` 확인
- [x] `doc/dev-c/.person/reports/task-033/` 보고서 확인

## 작업 목표

- 관리자 물품 입출고 내역 화면에서 날짜 기간 검색을 지원한다.
- `GET /admin/item/history`, `GET /admin/item/use` 모두 `fromDate`, `toDate` 기준 조회를 지원한다.
- 기본값 `오늘 ~ 오늘`, 한쪽 날짜 누락, 날짜 역전, 날짜 형식 오류를 같은 규칙으로 검증한다.
- 날짜 검색과 함께 기존 클라이언트 필터 상태(`keyword`, `type`, `category`)를 유지한다.
- 관련 테스트와 `workflow-033`, `task-033` 문서를 현재 구현 기준으로 완료 상태까지 정리한다.

## 보고서 소스

- `report-20260323-1742-task-33-1.md`
- `report-20260323-1746-task-33-2.md`
- `report-20260323-1756-task-33-3.md`
- `report-20260323-1819-task-33-4.md`
- `report-20260323-1826-task-33-5.md`
- `report-20260323-1902-task-33-6.md`
- `report-20260323-1912-task-33-7.md`
- `report-20260323-1948-task-33-8.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/item/AdminItemController.java`
- `src/main/java/com/smartclinic/hms/item/ItemManagerService.java`
- `src/main/java/com/smartclinic/hms/item/log/ItemStockLogRepository.java`
- `src/main/java/com/smartclinic/hms/item/log/ItemUsageLogRepository.java`
- `src/main/resources/templates/admin/item-history.mustache`
- `src/main/resources/templates/admin/item-use.mustache`
- `src/test/java/com/smartclinic/hms/admin/item/AdminItemControllerTest.java`
- `src/test/java/com/smartclinic/hms/item/ItemManagerServiceTest.java`
- `doc/dev-c/task/task-033.md`
- `doc/dev-c/workflow/workflow-033.md`

## 구현 내용

### 1. 날짜 검색 계약과 검증 규칙 고정

- 초기 구조 점검에서 `item-history`는 `ItemStockLog` 기준의 통합 입출고 이력 화면이고, `item-use`는 출고 실행 화면이면서 상단 로그 영역으로 날짜 기준 출고 내역을 함께 조회하는 구조임을 확인했다.
- 요청 계약은 `fromDate`, `toDate` 두 값으로 고정했고, 두 값이 모두 없을 때만 `오늘 ~ 오늘` 기본값을 적용하도록 정리했다.
- 한쪽 날짜만 입력한 경우, `fromDate > toDate`인 경우, 날짜 형식이 잘못된 경우는 모두 검증 에러로 닫도록 컨트롤러 책임을 명확히 했다.
- 실제 조회 범위는 `[fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())` 기준으로 맞춰 단일일 조회와 기간 조회를 같은 방식으로 처리했다.

### 2. 입고 내역 날짜 기간 검색 구현

- `AdminItemController.history()`에 날짜 파라미터 해석, 기본값 주입, 검증 실패 시 same-view `dateError` 반환 로직을 추가했다.
- `ItemManagerService`와 `ItemStockLogRepository`에는 날짜 범위 기준 입출고 로그 조회와 총 입고량, 총 출고량 계산 로직을 추가했다.
- 이 단계에서 입고 내역 화면은 날짜 범위에 맞는 통합 입출고 이력과 합계를 서버에서 계산하는 구조로 정리됐다.

### 3. 출고 내역 날짜 기간 검색 구현

- `AdminItemController.itemUsePage()`와 `POST /admin/item/use`에도 같은 날짜 범위 처리 규칙을 적용했다.
- `ItemManagerService`와 `ItemUsageLogRepository`는 날짜 범위 기준 출고 로그와 총 출고량을 조회하도록 확장됐다.
- 출고 실행 후 AJAX 응답도 현재 선택한 `fromDate`, `toDate` 기준으로 로그를 다시 반환하게 맞춰, 화면 갱신 뒤에도 사용자가 보던 기간 맥락이 유지되도록 했다.

### 4. 검색 UI와 SSR 기본값 반영

- `item-history.mustache`, `item-use.mustache` 상단에 같은 패턴의 날짜 검색 바를 추가했다.
- 두 화면 모두 SSR 렌더링에서 `fromDate`, `toDate` 기본값이 바로 보이도록 `오늘 ~ 오늘` 값을 컨트롤러에서 주입했다.
- `item-use` 출고 폼에는 현재 날짜 범위를 hidden 값으로 함께 실어 보내 비동기 출고 후에도 같은 범위를 유지하게 했다.
- 초기화 버튼은 무조건 전체 조회가 아니라 기본 URL로 복귀한 뒤 컨트롤러가 다시 `오늘 ~ 오늘`을 넣는 흐름으로 정리됐다.

### 5. 날짜 조건과 클라이언트 보조 필터 상태 유지

- 서버는 날짜 범위만 책임지고, 기존 `keyword`, `type`, `category` 필터는 클라이언트가 계속 담당하도록 역할을 분리했다.
- 날짜 검색 폼에는 hidden field를 추가해 현재 클라이언트 필터 상태를 함께 전달할 수 있게 했다.
- `window.history.replaceState`를 사용해 클라이언트 필터 변경 시 URL query string을 같이 갱신하고, 새로고침이나 날짜 재조회 후에도 같은 상태를 복원하도록 정리했다.
- 클라이언트 필터가 바뀌면 페이지 번호를 1로 되돌려 날짜 범위는 같지만 빈 페이지가 보이는 문제를 막았다.

### 6. 테스트 보강과 문서 마감

- `AdminItemControllerTest`에는 역전 날짜, 잘못된 날짜 형식, 한쪽 날짜만 입력한 경우, `POST /admin/item/use`의 잘못된 날짜 범위 400 응답 등 에러 케이스를 추가했다.
- `ItemManagerServiceTest`에는 `getTodayStaffUsageLogs()`가 실제 오늘 시작 시각부터 다음 날 시작 시각 전까지 범위를 사용하는지 검증을 보강했다.
- 인코딩 환경 차이로 테스트가 흔들리지 않도록, 에러 문구 문자열보다 `dateError` 존재 여부, HTTP 상태, 서비스 호출 여부 같은 계약 중심으로 검증을 고정했다.
- 마지막 단계에서 `workflow-033`, `task-033`를 현재 구현과 최종 테스트 결과에 맞춰 갱신했고, 전체 `./gradlew test`까지 재실행해 문서와 코드 상태를 함께 닫았다.

## 검증 결과

- 구조 점검/설계 단계(`Task 33-1`, `Task 33-2`)는 문서 정리 중심이라 별도 테스트 실행 없음
- `./gradlew test --tests 'com.smartclinic.hms.admin.item.AdminItemControllerTest' --tests 'com.smartclinic.hms.item.ItemManagerServiceTest'` : `BUILD SUCCESSFUL`
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.item.AdminItemControllerTest'` : `BUILD SUCCESSFUL`
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.item.ItemManagerServiceTest'` : `BUILD SUCCESSFUL`
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.item.AdminItemControllerTest' --tests 'com.smartclinic.hms.item.ItemManagerServiceTest'` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.item.*' --tests 'com.smartclinic.hms.item.*'` : `BUILD SUCCESSFUL`
- `./gradlew cleanTest test --tests 'com.smartclinic.hms.admin.item.*' --tests 'com.smartclinic.hms.item.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-033.md`
- 로컬: `doc/dev-c/workflow/workflow-033.md`

## 남은 TODO / 리스크

- 현재 보고서, `task-033`, `workflow-033` 기준으로 즉시 남은 TODO는 없다.
- 관련 범위 테스트와 전체 테스트가 모두 통과한 상태라 이번 묶음은 완료로 봐도 된다.
- 다만 이후 검색 구조를 다시 손볼 때는 서버 날짜 필터와 클라이언트 보조 필터의 책임 분리를 함께 유지해야 회귀를 줄일 수 있다.
