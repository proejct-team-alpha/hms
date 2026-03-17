# 직원 수정 SSR 구현 명세서 (workflow-013)

## 문제 정의
관리자 직원 수정 기능을 구현한다. `GET /admin/staff/detail?staffId={id}`에서 수정 폼을 제공하고, `POST /admin/staff/update`에서 직원 정보를 수정한다.

## 대상 사용자
- 관리자(`ROLE_ADMIN`)

## 전달 요구사항
1. `GET /admin/staff/detail?staffId={id}` 수정 화면 제공
2. `POST /admin/staff/update` 수정 처리
3. 수정 가능 필드
   - 이름
   - 부서
   - 비밀번호(입력 시만 변경)
4. 읽기 전용 표시 필드
   - 로그인 아이디
   - 사번
   - 역할
5. 일반 수정 범위에서 제외
   - 재직 상태
   - 별도 비활성화 기능으로 분리
6. 역할이 `DOCTOR`인 경우 추가 수정 가능
   - 전문 분야
   - 진료 가능 요일
7. 수정 성공 후 직원 목록으로 리다이렉트

## 제약 조건
- URL은 기존에 정해진 주소를 그대로 사용
  - `GET /admin/staff/detail?staffId={id}`
  - `POST /admin/staff/update`
- SSR 방식 유지
- 등록/수정 폼 템플릿은 최대한 재사용 가능하게 설계
- 비밀번호는 입력값이 있을 때만 BCrypt로 갱신
- `DOCTOR`가 아닌 경우 의사 전용 필드는 서버에서 무시 또는 비노출 처리

## 엣지 케이스
- 존재하지 않는 직원 ID
- 비밀번호 공백 입력
- 잘못된 부서 ID
- `DOCTOR`가 아닌데 전문 분야/요일 값이 들어오는 경우
- 수정 대상이 `DOCTOR`인데 연결된 `Doctor` 엔티티가 없는 경우

## 수용 기준
- [ ] `/admin/staff/detail?staffId={id}` 접속 시 수정 폼이 렌더링된다
- [ ] 이름/부서 수정이 가능하다
- [ ] 비밀번호는 입력한 경우에만 변경된다
- [ ] 로그인 아이디/사번/역할은 읽기 전용으로 보인다
- [ ] `DOCTOR` 직원은 전문 분야/진료 가능 요일 수정이 가능하다
- [ ] 수정 성공 후 목록으로 리다이렉트된다

## 구현 순서
1. 직원 수정에 필요한 `Staff`, `Doctor`, `Department`, `StaffRole` 구조 확인
2. 수정 요청 DTO와 수정 폼 렌더링용 응답 DTO 설계
3. `AdminStaffRepository` 및 관련 조회 리포지토리 보강
4. `AdminStaffService`에 직원 수정 로직 구현
5. `AdminStaffController`에 `GET /admin/staff/detail`, `POST /admin/staff/update` 구현
6. `staff-form.mustache`를 등록/수정 겸용 구조로 정리
7. Controller/Service 테스트 작성 및 검증
