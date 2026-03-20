# task-025 작업 로그

## 작업 전 준수 항목 체크리스트

- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-025.md` 확인
- [x] `doc/dev-c/workflow/workflow-025.md` 확인
- [x] `doc/dev-c/.person/reports/task-025/` 보고서 확인

## 작업 목표

- 관리자 병원 규칙 목록 화면 `GET /admin/rule/list`를 검색형 목록 화면으로 완성한다.
- 카테고리 필터, 활성 여부 필터, 제목 키워드 검색, 페이지네이션, 활성 상태 배지를 제공한다.
- 필터/검색 조건을 유지한 채 페이지 이동이 가능하도록 목록 응답 구조와 URL 생성을 정리한다.
- 컨트롤러/서비스 테스트와 전체 테스트로 `admin.rule` 범위 회귀를 방어한다.

## 보고서 소스

- `report-20260319-1828-task-25-1.md`
- `report-20260319-1834-task-25-2.md`
- `report-20260319-1844-task-25-3.md`
- `report-20260319-1858-task-25-4.md`
- `report-20260319-1904-task-25-5.md`
- `report-20260319-1930-task-25-6.md`
- `report-20260319-1942-rule-pagination-window-refactor.md`
- `report-20260319-1947-rule-list-lightweight-refactor.md`

## 변경 파일

- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleController.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleService.java`
- `src/main/java/com/smartclinic/hms/admin/rule/HospitalRuleRepository.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleDto.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRulePageLinkResponse.java`
- `src/main/java/com/smartclinic/hms/admin/rule/AdminRuleFilterOptionResponse.java`
- `src/main/resources/templates/admin/rule-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/rule/AdminRuleServiceTest.java`
- `doc/dev-c/task/task-025.md`
- `doc/dev-c/workflow/workflow-025.md`

## 구현 내용

### 1. 기존 규칙 목록 구조 점검과 설계 기준 정리

- 기존 `AdminRuleController`는 단순 전체 조회만 수행했고, `page`, `size`, `category`, `active`, `keyword` 같은 검색형 목록 파라미터를 받지 않는 상태였다.
- `AdminRuleService`와 `HospitalRuleRepository`도 전체 조회 기준이라, 필터/검색/페이지네이션을 붙이려면 목록 응답 구조와 조회 책임을 먼저 분리해야 했다.
- `AdminRuleDto`는 `categoryText`, `activeText`, `activeBadgeClass`를 이미 잘 담고 있어 row DTO는 유지하고, 바깥쪽 목록 응답을 새로 만드는 방향으로 설계를 고정했다.

### 2. 목록 응답 DTO와 조회 책임 설계

- `AdminRuleListResponse`를 추가해 규칙 목록, 페이지 링크, 현재 선택된 필터, 총 건수, 이전/다음 URL, 페이지 메타데이터를 한 번에 감싸도록 정리했다.
- `AdminRulePageLinkResponse`로 페이지 번호/URL/활성 여부를 분리해 Mustache 페이지네이션 UI가 단순 반복 렌더링으로 동작하게 했다.
- 이후 템플릿 `selected` 처리를 위해 `AdminRuleFilterOptionResponse`도 추가해 카테고리/활성 옵션의 선택 상태를 응답에서 미리 계산하도록 맞췄다.
- 기본 파라미터는 `page=1`, `size=10`, `category=ALL`, `active=ALL`, `keyword=''`로 고정했다.

### 3. 검색 + 필터 + 페이지네이션 백엔드 구현

- `AdminRuleController`는 `page`, `size`, `category`, `active`, `keyword`를 받아 서비스로 전달하도록 확장했다.
- `AdminRuleService`는 잘못된 파라미터를 기본값으로 정규화하고, 현재 조건을 유지하는 페이지 링크와 이전/다음 URL을 조합하도록 구현했다.
- `HospitalRuleRepository`에는 카테고리, 활성 여부, 제목 키워드를 한 번에 처리하는 `search(...)` 쿼리를 추가했다.
- 제목 검색은 `lower(title) like lower('%keyword%')`, 정렬은 `createdAt desc, id desc` 기준으로 맞췄다.
- 기존 템플릿 호환을 위해 `rules`, `hasRules`도 함께 유지하면서 새 목록 응답 구조를 도입했다.

### 4. 규칙 목록 Mustache 화면 완성

- `rule-list.mustache`를 UTF-8 기준으로 다시 정리하고, 제목 검색 입력창, 카테고리 셀렉트, 활성 여부 셀렉트, 조회/초기화 버튼을 갖춘 검색 바를 추가했다.
- 각 row에는 카테고리 텍스트, 활성 상태 텍스트, 활성 배지를 붙여 관리 화면 가독성을 높였다.
- 빈 결과일 때는 `조회된 규칙이 없습니다.` 메시지를 보여주고, 필터/검색 조건을 유지한 페이지네이션 UI를 하단에 배치했다.
- 최종 리팩토링에서는 카드형 목록에서 테이블형 목록으로 바꾸고, 목록 컬럼을 제목/카테고리/상태 3개만 남겨 검색형 인덱스 화면으로 더 가볍게 정리했다.
- 규칙 본문(content) 미리보기는 목록에서 제거해 읽기 화면이 아니라 빠르게 찾는 목록 역할에 집중시켰다.

### 5. 테스트 보강과 최종 화면 다듬기

- `AdminRuleControllerTest`에는 파라미터 전달, 검색/필터 바 렌더링, 빈 결과 메시지, 선택된 필터 표시, 페이지 번호 렌더링을 추가했다.
- `AdminRuleServiceTest`에는 필터 적용 결과, 파라미터 기본값 보정, 페이지네이션 메타데이터, 필터 유지 URL, 옵션 라벨, 빈 결과 메타데이터를 보강했다.
- Mustache 렌더링에서 URL 이스케이프(`&#x3D;`) 문제를 피하려고 전체 링크 문자열 비교보다 의미 중심 검증으로 테스트를 안정화했다.
- 이후 추가 리팩토링으로 페이지 번호 버튼을 전부 노출하지 않고 최대 5개만 보이는 슬라이딩 윈도우 방식으로 바꿔 탐색 집중도를 높였다.

### 6. 문서와 검증 마무리

- `task-025.md`, `workflow-025.md`를 현재 구현 상태에 맞게 완료 처리했다.
- report 중 `task-25-6` 파일은 일부 인코딩 손상 흔적이 있었지만, `task-025.md`와 `workflow-025.md`는 완료 기준과 리뷰 포인트를 정상적으로 확인할 수 있었다.
- 최종적으로 `admin.rule` 범위 테스트와 전체 `./gradlew test`를 다시 실행해 마무리 상태를 확인했다.

## 검증 결과

- 구조 점검 단계: 설계 확인 중심이라 별도 테스트 실행 없음
- `./gradlew compileJava` : `BUILD SUCCESSFUL`
- `./gradlew test --tests 'com.smartclinic.hms.admin.rule.*'` : `BUILD SUCCESSFUL`
- `./gradlew test` : `BUILD SUCCESSFUL`
- 참고:
- 자바/템플릿/테스트 파일 저장 중 BOM 이슈가 한 번 있었고, UTF-8 without BOM으로 재저장 후 정상 통과
- `task-025.md`는 중간에 인코딩이 깨져 한글을 복구한 뒤 계속 사용했다

## 참고 문서

- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-025.md`
- 로컬: `doc/dev-c/workflow/workflow-025.md`

## 남은 TODO / 리스크

- 현재 `task-025` 기준 필수 구현은 완료 상태다.
- Repository 단일 `search(...)` 쿼리는 현재 범위에는 충분하지만, 향후 검색 조건이 더 늘어나면 별도 Repository 테스트나 조회 구조 재분리가 필요할 수 있다.
- 규칙 상세/수정 화면이 추가되면 현재 목록에서 제거한 본문 정보는 그쪽 책임으로 넘기는 방향이 더 자연스럽다.
