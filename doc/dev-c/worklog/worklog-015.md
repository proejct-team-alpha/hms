# worklog-015

## 준수 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] 체크리스트 먼저 출력 후 구현 진행

## 구현 내용
1. `CreateAdminStaffRequest`
- 의사용 생성 입력을 받기 위해 `specialty`, `availableDays` 필드를 추가했다.

2. `AdminStaffService`
- 직원 생성 시 역할이 `DOCTOR`이면 `Doctor` 엔티티를 함께 생성하도록 리팩토링했다.
- 생성/수정 폼 응답에서 의사용 필드(`doctorRole`, `specialty`, `availableDayOptions`)를 일관되게 구성하도록 정리했다.
- 의사 생성/수정 시 부서가 없는 상태를 막기 위해 방어 로직을 추가했다.

3. `staff-form.mustache`
- 등록 폼에서도 `ROLE_DOCTOR` 선택 시 의사용 입력 영역이 보이도록 구조를 정리했다.
- 기존 수정 폼에서 쓰던 전문 분야/진료 가능 요일 UI를 등록 폼에도 재사용했다.
- 역할 선택 변경 시 의사용 영역이 즉시 토글되도록 스크립트를 추가했다.

4. 테스트
- `AdminStaffServiceTest`에 의사 생성 시 `Doctor` 엔티티 동시 생성 검증을 추가했다.
- `AdminStaffControllerTest`에 의사 생성 파라미터 바인딩과 등록 실패 재렌더링 흐름을 반영했다.
- 인코딩 영향이 큰 테스트 문자열은 안정적으로 검증되도록 정리했다.

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/staff/dto/CreateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/resources/templates/admin/staff-form.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`

## 참조 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 작업 명세: `doc/dev-c/workflow/workflow-015.md`

## 검증 결과
- 실행: `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'`
- 결과: `BUILD SUCCESSFUL`

## 메모
- Java 소스/테스트 일부는 기존 인코딩 손상 영향이 커서, 이번 작업에서는 기능 안정성과 테스트 통과를 우선해 관련 파일을 정리했다.
- 템플릿의 사용자 노출 문구는 후속으로 UTF-8 기준 일괄 정리하는 편이 안전하다.