# 직원 생성 시 DOCTOR 엔티티 동시 생성 리팩토링 명세서 (workflow-015)

## 문제 정의
`AdminStaffService`를 리팩토링해 직원 생성 시 역할이 `DOCTOR`이면 `Staff` 엔티티와 함께 `Doctor` 엔티티도 생성하여 1:1 매핑이 완성된 상태로 저장한다. 등록 폼은 기존 수정 폼에서 사용 중인 의사용 필드 구조를 재사용한다.

## 대상 사용자
- 관리자(`ROLE_ADMIN`)

## 전달 요구사항
1. 직원 생성 로직 리팩토링
2. `role == DOCTOR`이면 `Staff` 생성 후 `Doctor` 엔티티도 함께 생성
3. `Doctor` 엔티티는 생성 시 `Staff`와 1:1로 연결
4. 등록 폼에서도 `ROLE_DOCTOR` 선택 시 의사용 필드 노출
   - 전문 분야
   - 진료 가능 요일
5. 수정 폼에서 이미 사용 중인 의사용 필드 구조를 등록 폼에도 재사용
6. `DOCTOR`가 아닌 역할이면 전문 분야/진료 가능 요일 값은 무시

## 제약 조건
- 기존 등록 URL 유지
  - `GET /admin/staff/new`
  - `POST /admin/staff/create`
- 기존 수정 URL 유지
  - `GET /admin/staff/detail?staffId={id}`
  - `POST /admin/staff/update`
- 등록/수정 폼은 가능한 한 동일한 템플릿 구조 유지
- 비밀번호 암호화, 로그인 아이디/사번 중복 검증 규칙 유지
- `Doctor` 생성 시 부서는 `Staff.department`와 일관되게 맞춤

## 엣지 케이스 & 에러 시나리오
- `DOCTOR` 선택 시 전문 분야/진료 가능 요일이 비어 있는 경우 처리 정책 확인 필요
- `DOCTOR`가 아닌데 의사용 필드 값이 넘어오는 경우 무시
- 부서가 없는 의사 생성 허용 여부 확인 필요
- 생성 이후 수정 화면에서 `Doctor` 엔티티가 없는 예외 케이스는 이번 리팩토링으로 제거하는 방향

## 기대 효과
- `DOCTOR` 직원 생성 직후부터 `Doctor` 엔티티가 보장됨
- 수정 로직에서 `DOCTOR`인데 `Doctor`가 없는 예외 상황 감소
- 등록/수정 폼 재사용성 향상
- 도메인 정합성 개선

## 수용 기준
- [ ] `ROLE_DOCTOR`로 직원 등록 시 `Staff`와 `Doctor`가 함께 생성된다
- [ ] 생성된 `Doctor`가 해당 `Staff`와 1:1로 연결된다
- [ ] 등록 폼에서 `ROLE_DOCTOR` 선택 시 전문 분야/진료 가능 요일 입력 UI가 보인다
- [ ] `DOCTOR`가 아닌 역할은 `Doctor` 엔티티를 생성하지 않는다
- [ ] 기존 직원 등록/수정 테스트가 갱신되고 통과한다

## 구현 순서
1. `CreateAdminStaffRequest`, `AdminStaffFormResponse`, `staff-form.mustache`의 의사용 필드 재사용 구조 확인
2. 등록 폼에서 `ROLE_DOCTOR` 선택 시 의사용 입력 필드 노출 방식 정리
3. `AdminStaffService#createStaff`에 `Doctor` 동시 생성 로직 추가
4. `DoctorRepository` 및 관련 도메인 생성 흐름 점검
5. 등록/수정 테스트를 역할별 생성 흐름 기준으로 갱신
6. 전체 직원 모듈 테스트 검증
