# task-017 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/task/task-017.md` 확인
- [x] report 폴더 기준 누적 요약 작성

## 작업 목표
- 관리자 진료과 목록 화면을 SSR 기준으로 정상화한다.
- `admin.department` 모듈 책임에 맞게 조회 구조를 정리한다.
- 전체 조회를 페이지 목록 구조로 전환하고, UI까지 일관되게 연결한다.
- 등록 폼의 입력 계약을 화면과 서버 사이에서 맞춘다.
- `admin.department` 범위 테스트를 보강해 회귀를 막는다.

## 보고서 소스
- `doc/dev-c/.person/reports/task-017/report-20260318-1042-task-17-1.md`
- `doc/dev-c/.person/reports/task-017/report-20260318-1108-task-17-2.md`
- `doc/dev-c/.person/reports/task-017/report-20260318-1117-task-17-3.md`
- `doc/dev-c/.person/reports/task-017/report-20260318-1144-task-17-4.md`
- `doc/dev-c/.person/reports/task-017/report-20260318-1152-task-17-5.md`
- `doc/dev-c/.person/reports/task-017/report-20260318-1201-task-17-6.md`

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentController.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentDto.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentPageLinkResponse.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentRepository.java`
- `src/main/java/com/smartclinic/hms/admin/department/AdminDepartmentService.java`
- `src/main/resources/templates/admin/department-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/department/AdminDepartmentControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/department/AdminDepartmentServiceTest.java`

## 구현 내용
1. 목록 화면 기본 렌더링을 먼저 복구했다.
- `department-list.mustache`의 깨진 한글 문구와 손상된 마크업을 정리했다.
- `{{pageTitle}}` 기반 화면 제목 구조를 적용하고, 빈 목록 메시지와 상태 배지 문구를 정상화했다.
- 화면에서 사용하는 `activeText`, `activeBadgeClass` 같은 상태 라벨 계약도 함께 정리했다.

2. 진료과 목록 조회 책임을 `admin.department` 모듈로 되돌렸다.
- `AdminDepartmentService`가 더 이상 `reservation` 쪽 리포지토리에 의존하지 않도록 수정했다.
- `AdminDepartmentRepository`에 이름 오름차순 목록 조회 메서드를 추가하고, 서비스는 이 결과를 화면 DTO로 변환하는 역할만 맡도록 정리했다.
- 서비스 단위 테스트를 추가해 조회 책임 이전이 고정되도록 했다.

3. 목록 백엔드에 페이지네이션 응답 모델을 도입했다.
- 컨트롤러가 `page`, `size` 파라미터를 받고 `model + pageTitle` 패턴으로 화면에 전달하도록 변경했다.
- 서비스에서 `PageRequest`를 사용해 `totalCount`, `currentPage`, `totalPages`, `hasPrevious`, `hasNext`, `previousUrl`, `nextUrl`, `pageLinks`를 계산하도록 정리했다.
- 이를 위해 `AdminDepartmentListResponse`, `AdminDepartmentPageLinkResponse`를 추가해 SSR 템플릿이 필요한 메타정보를 한 번에 받을 수 있게 했다.

4. Mustache 화면에 페이지네이션 UI를 반영했다.
- `model.departments` 기준 렌더링을 유지하면서 총 건수, 현재 페이지, 이전/다음 링크, 페이지 번호 링크를 추가했다.
- 백엔드가 계산한 값을 템플릿이 그대로 소비하는 구조로 유지해, 화면에서 직접 페이지 계산을 하지 않도록 했다.
- 렌더링 테스트는 URL 전체 문자열보다 실제 페이지 정보와 번호 링크 표시를 기준으로 검증하도록 다듬었다.

5. 등록 폼과 서버 입력 계약을 일치시켰다.
- 등록 모달에서 보내는 `active` 체크박스 값을 컨트롤러와 서비스가 실제로 받도록 `create(String name, boolean active)` 흐름으로 변경했다.
- `@RequestParam(defaultValue = "false")`를 적용해 체크되지 않은 경우에도 `false`가 명확히 전달되도록 했다.
- 등록 후 목록 복귀는 `RedirectView`와 `setExposeModelAttributes(false)`를 사용해 URL 쿼리스트링에 불필요한 모델 값이 붙지 않도록 정리했다.

6. 테스트를 보강하면서 숨은 렌더링 회귀도 함께 수정했다.
- 컨트롤러 테스트에 빈 목록 상태와 페이지 정보 렌더링 검증을 추가했다.
- 서비스 테스트에는 빈 데이터셋과 페이지 링크 계산 케이스를 추가했다.
- 이 과정에서 빈 목록일 때 `1 / 0페이지`처럼 잘못 보이던 문제를 발견했고, 응답 모델에 `hasPages` 플래그를 추가해 템플릿이 `0 / 0페이지`를 안정적으로 렌더링하도록 고쳤다.

## 검증 결과
- 실행 명령어: `./gradlew test`
- 결과: `BUILD SUCCESSFUL`
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.department.*'`
- 결과: `BUILD SUCCESSFUL`

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/task/task-017.md`
- 로컬: `doc/dev-c/workflow/workflow-017.md`

## 남은 TODO / 리스크
- report 기준으로 `task-017` 구현 범위는 완료 상태다.
- 후속 작업으로는 커밋, 리포트 정리, PR 작성 같은 마감 단계만 남아 있다.
