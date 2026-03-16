# workflow-012 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/workflow/workflow-012.md` 요구사항 확인
- [x] 체크리스트를 구현 전에 먼저 출력

## 작업 목표
- `GET /admin/staff/new` 등록 화면 제공
- `POST /admin/staff/create` 등록 처리
- 직원 정보 입력 후 DB 저장
- 비밀번호 BCrypt 암호화
- 등록 성공 후 직원 목록으로 리다이렉트
- 추후 수정 폼 재사용 가능한 구조 유지

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffRepository.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffDepartmentRepository.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/CreateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffFormResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffFormOptionResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffDepartmentOptionResponse.java`
- `src/main/resources/templates/admin/staff-form.mustache`
- `src/main/resources/templates/admin/staff-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`

## 구현 내용
1. `AdminStaffController`
- `GET /admin/staff/new` 등록 화면 렌더링 추가
- `POST /admin/staff/create` 등록 처리 추가
- 입력 검증 실패 시 폼 재렌더링 + 에러 메시지 노출
- 성공 시 직원 목록으로 리다이렉트 + flash `successMessage` 설정
- 기존 `/admin/staff/form` 경로는 현재 템플릿 호환을 위해 동일 폼으로 연결 유지

2. `AdminStaffService`
- 직원 등록용 폼 응답 DTO 생성 로직 추가
- 직원 등록 로직 추가
- BCrypt `PasswordEncoder`를 사용해 비밀번호 암호화 저장
- 로그인 아이디 중복, 사번 중복 검증 추가
- 역할/부서 유효성 검증 추가
- 등록 화면 기본 재직 상태를 `true`로 설정

3. `AdminStaffRepository` / `AdminStaffDepartmentRepository`
- `existsByUsername`, `existsByEmployeeNumber` 추가
- 활성 부서 목록 조회용 `AdminStaffDepartmentRepository` 추가
- 부서 선택은 활성 부서 기준으로 구성

4. DTO
- `CreateAdminStaffRequest`: 등록 요청 바인딩 및 검증
- `AdminStaffFormResponse`: 등록/수정 재사용 가능한 폼 모델
- `AdminStaffFormOptionResponse`: 역할/재직 상태 select 옵션
- `AdminStaffDepartmentOptionResponse`: 부서 select 옵션

5. 템플릿
- `staff-form.mustache`
  - JS 더미 기반 폼 제거
  - SSR POST 폼으로 전환
  - CSRF 토큰 포함
  - 로그인 아이디, 비밀번호, 이름, 사번, 역할, 부서, 재직 상태 입력 지원
- `staff-list.mustache`
  - 등록 버튼(`/admin/staff/new`) 추가
  - 등록 성공 flash 메시지 표시 추가

6. 테스트
- `AdminStaffServiceTest`
  - 직원 등록 시 BCrypt 암호화 저장 검증
  - 중복 로그인 아이디 예외 검증
  - 기존 목록 검색/페이징 테스트 유지
- `AdminStaffControllerTest`
  - 등록 화면 렌더링 검증
  - 등록 성공 리다이렉트 검증
  - 검증 실패 시 등록 화면 재렌더링 검증
  - 기존 목록 렌더링/파라미터 전달 테스트 유지
- 리다이렉트 URL 검증은 프로젝트 기존 인터셉터 동작에 맞게 `containsString` 기준으로 검증

## 검증 결과
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'`
- 결과: `BUILD SUCCESSFUL`

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/rules/rule-controller.md`
- 로컬: `doc/rules/rule-repository.md`
- 로컬: `doc/dev-c/workflow/workflow-012.md`

## 남은 TODO / 리스크
- 현재 `staff-form.mustache`는 등록 기준으로 구성되어 있으며, 수정 기능이 들어오면 비밀번호 변경 여부와 기존 값 노출 정책을 별도 정리해야 함
- 공통 인터셉터가 리다이렉트 모델 속성을 URL에 포함시키는 기존 패턴이 있어, 향후 PRG URL 정리 필요 시 공통 설정 차원에서 재검토 필요
- 부서 선택은 현재 활성 부서만 대상으로 하며, 역할별 부서 필수 여부는 후속 정책 확정 시 보완 가능
