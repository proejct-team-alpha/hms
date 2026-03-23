# Task 033 - 입출고 내역 날짜 기간 검색

## Task 33-1. 현재 입고/출고 내역 조회 구조 점검
- [x] admin 물품 내역 조회 구조와 화면 역할을 먼저 점검한다
- [x] 날짜 필터를 어느 Repository와 Service에 붙일지 확인한다
- [x] 입고 내역과 출고 내역 화면이 같은 방식으로 확장 가능한지 확인한다
- [x] 관련 테스트 위치와 범위를 정리한다

## Task 33-2. 날짜 검색 파라미터 및 검증 규칙 설계
- [x] `fromDate`, `toDate` 요청 계약을 고정한다
- [x] 기본값을 `오늘 ~ 오늘`로 정한다
- [x] 한쪽만 입력하면 검증 에러를 반환하도록 정한다
- [x] `fromDate > toDate` 검증 에러 규칙을 정한다
- [x] 날짜 범위 조회 경계와 화면별 처리 방식을 문서화한다

## Task 33-3. 입고 내역 날짜 기간 검색 구현
- [x] `/admin/item/history`에 날짜 기간 검색을 추가한다
- [x] 기본값/검증 규칙을 컨트롤러에 반영한다
- [x] Service/Repository 날짜 범위 조회 및 합계 로직을 구현한다

## Task 33-4. 출고 내역 날짜 기간 검색 구현
- [x] `/admin/item/use`에 날짜 기간 검색을 추가한다
- [x] 출고 실행 후 갱신되는 로그도 현재 날짜 범위를 기준으로 다시 조회한다
- [x] use API에서도 `fromDate`, `toDate`를 받아 같은 기준으로 로그를 반환한다
- [x] Service/Repository 날짜 범위 조회 및 합계 로직을 구현한다

## Task 33-5. 검색 UI 및 기본값 반영
- [x] `item-history` 상단에 날짜 검색 UI를 추가한다
- [x] `item-use` 상단에도 같은 패턴의 날짜 검색 UI를 추가한다
- [x] `오늘 ~ 오늘` 기본값이 SSR 렌더링에서 바로 보이게 연결한다
- [x] 조회/초기화 버튼과 핵심 문구 렌더링을 테스트로 확인한다

## Task 33-6. 날짜 조건 유지 및 클라이언트 필터 상태 정리
- [x] 날짜 검색 폼 제출 시 현재 클라이언트 필터 상태가 함께 유지되도록 정리한다
- [x] `item-history`에서 `keyword`, `type` 상태를 URL과 hidden field에 동기화한다
- [x] `item-use`에서 `keyword`, `category` 상태를 URL과 hidden field에 동기화한다
- [x] 클라이언트 필터 변경 시 페이지 번호를 1로 되돌리고 URL을 함께 갱신한다
- [x] 관련 컨트롤러 테스트에 상태 유지용 hidden field 렌더링 검증을 추가한다

## Task 33-7. 테스트 보강
- [x] 날짜 검증, 합계, API 에러 처리 흐름 테스트를 보강한다
- [x] 화면 렌더링과 JS 상태 유지의 핵심 포인트를 추가 검증한다
- [x] 입출고 범위 테스트를 다시 통과시킨다

## Task 33-8. 문서 및 최종 검증 마무리
- [x] workflow-033을 현재 구현 상태 기준으로 갱신한다
- [x] task-033 전체 완료 기준을 갱신한다
- [x] 구현 점검 결과와 리뷰 포인트를 정리한다
- [x] 최종 테스트 결과를 문서에 반영한다

## 전체 완료 기준
- [x] 입고 내역 조회에서 날짜 기간 검색이 동작한다
- [x] 출고 내역 조회에서 날짜 기간 검색이 동작한다
- [x] 기본값 `오늘 ~ 오늘`이 SSR 기준으로 반영된다
- [x] 한쪽 날짜만 입력하면 검증 에러가 표시된다
- [x] `fromDate > toDate`면 검증 에러가 표시된다
- [x] 날짜 형식 오류 시 검증 에러가 표시된다
- [x] 날짜 검색 UI가 두 화면 모두 같은 패턴으로 정리된다
- [x] 날짜 조건과 기존 클라이언트 필터 상태를 함께 유지할 수 있다
- [x] 관련 범위 테스트와 전체 테스트를 최종 확인한다

## Task 33-1 점검 메모
- `GET /admin/item/history`는 `ItemStockLog` 기준의 통합 입출고 이력 화면이다.
- `GET /admin/item/use`는 출고 실행 화면이지만, 상단 `todayLogs` 영역을 통해 날짜 기준 출고 내역 조회를 함께 수행할 수 있다.
- `ItemStockLogRepository`, `ItemUsageLogRepository` 모두 날짜 범위 조회로 확장 가능한 구조다.
- admin item 테스트는 `src/test/java/com/smartclinic/hms/admin/item`와 `src/test/java/com/smartclinic/hms/item` 아래에 나뉘어 있다.

## Task 33-2 설계 메모
- 날짜 파라미터는 `fromDate`, `toDate` 두 개를 사용한다.
- 형식은 `yyyy-MM-dd`이며, 컨트롤러에서는 `LocalDate`로 다룬다.
- 기본값은 두 값이 모두 없을 때만 `오늘 ~ 오늘`을 적용한다.
- 한쪽만 입력되면 자동 보정하지 않고 검증 에러를 반환한다.
- datetime 범위는 `[fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())` 기준으로 조회한다.

## Task 33-3 구현 메모
- `AdminItemController.history()`에 날짜 파라미터 해석과 검증 로직을 추가했다.
- `ItemManagerService`, `ItemStockLogRepository`에 날짜 범위 조회 및 입고/출고 합계 로직을 추가했다.
- 컨트롤러와 서비스 테스트를 통해 기본값, 검증, 범위 조회를 확인했다.

## Task 33-4 구현 메모
- `AdminItemController.itemUsePage()`와 `POST /admin/item/use`에 날짜 범위 처리 로직을 추가했다.
- `ItemManagerService`, `ItemUsageLogRepository`에 날짜 범위 출고 로그 및 합계 조회를 추가했다.
- 출고 실행 후 AJAX 응답도 현재 날짜 범위 기준 로그를 다시 반환하도록 맞췄다.

## Task 33-5 구현 메모
- `item-history.mustache`, `item-use.mustache` 상단에 날짜 검색 UI를 동일한 패턴으로 추가했다.
- `오늘 ~ 오늘` 기본값이 SSR에서 바로 보이도록 `fromDate`, `toDate` 값을 연결했다.
- `item-use` 출고 폼에는 현재 날짜 범위를 hidden 값으로 실어 보내 비동기 출고 후에도 같은 맥락을 유지하게 했다.
- `AdminItemControllerTest`에 날짜 검색 UI 렌더링 검증을 추가했다.

## Task 33-6 구현 메모
- 두 화면 모두 서버는 날짜 범위만 처리하고, 기존 `keyword`/`type`/`category` 필터는 클라이언트에서 계속 처리하도록 역할을 나눴다.
- 날짜 검색 폼에 hidden field를 추가해 현재 클라이언트 필터 상태를 같이 제출할 수 있게 했다.
- `window.history.replaceState`를 이용해 클라이언트 필터 변경 시 URL query string도 함께 갱신하도록 정리했다.
- 새로고침이나 날짜 재조회 후에도 JS가 URL의 `keyword`, `type`, `category`를 다시 읽어 같은 화면 상태를 복원한다.
- 클라이언트 필터가 바뀌면 페이지 번호를 1로 되돌려 빈 페이지가 보이지 않도록 맞췄다.

## Task 33-7 구현 메모
- `AdminItemControllerTest`에 날짜 검증 케이스를 더 추가했다.
  - `history` 역전 날짜 에러
  - `history` 잘못된 날짜 형식 에러
  - `use` 한쪽 날짜만 입력한 에러
  - `use` API 역전 날짜 400 응답
- `ItemManagerServiceTest`에는 `getTodayStaffUsageLogs()`가 실제로 오늘 범위를 사용하는지 검증을 추가했다.
- 에러 문구는 인코딩 차이로 테스트가 흔들릴 수 있어, 이번 단계는 `dateError` 존재 여부와 HTTP 상태, 서비스 호출 여부를 중심으로 검증했다.

## Task 33-8 구현 메모
- `workflow-033`, `task-033`를 현재 구현 상태 기준으로 다시 정리했다.
- item 날짜 검색 작업은 서버 날짜 필터 + 클라이언트 보조 필터 구조라는 점을 문서에 명확히 남겼다.
- 범위 테스트와 전체 테스트 결과를 문서에 함께 기록해 PR 전 확인 포인트를 줄였다.