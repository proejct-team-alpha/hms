# 직원 등록 SSR 구현 명세서 (workflow-012)

## 문제 정의

관리자 직원 등록 기능을 구현한다. `GET /admin/staff/new`, `POST /admin/staff/create`에서 직원 등록 폼을 제공하고, 입력값을 받아 DB에 저장하며 비밀번호는 BCrypt로 암호화한다.

## 대상 사용자

- 관리자(`ROLE_ADMIN`)

## 전달 요구사항

1. `GET /admin/staff/new` 등록 화면 제공
2. `POST /admin/staff/create` 등록 처리
3. 입력 필드
   - 로그인 아이디
   - 비밀번호
   - 이름
   - 사번
   - 역할
   - 부서
   - 재직 상태
4. 비밀번호는 BCrypt로 암호화 후 저장
5. 등록 성공 후 직원 목록으로 리다이렉트
6. 등록/수정 폼은 추후 최대한 같은 템플릿 구조로 재사용 가능하게 설계
7. 등록 화면의 재직 상태 기본값은 `true`

## 제약 조건

- 관리자 URL 규칙 유지: `/admin/**`
- SSR 방식 유지
- DTO 네이밍 규칙 준수
- 서버 저장 시 보안 규칙에 맞게 암호화 처리
- 가능하면 현재 `staff-form.mustache`를 재사용 가능한 방향으로 정리

## 엣지 케이스

- 로그인 아이디 중복
- 사번 중복
- 필수값 누락
- 잘못된 역할/부서 선택
- 비밀번호 공백 또는 형식 미달

## 수용 기준

- [ ] `/admin/staff/new` 접속 시 등록 폼이 렌더링된다
- [ ] 입력값으로 직원 등록이 가능하다
- [ ] 비밀번호가 BCrypt로 암호화되어 저장된다
- [ ] 등록 성공 후 목록 화면으로 리다이렉트된다
- [ ] 중복/검증 실패 시 적절히 처리된다
- [ ] 추후 수정 폼 재사용 가능한 구조를 유지한다

## 구현 순서

1. 직원 등록에 필요한 `Staff`, `Department`, `StaffRole` 구조 확인
2. 등록 요청 DTO와 폼 렌더링용 응답 DTO 설계
3. `AdminStaffRepository`에 중복 검증/저장에 필요한 메서드 추가
4. `AdminStaffService`에 직원 등록 로직 구현
5. `AdminStaffController`에 `GET/POST /admin/staff/new` 구현
6. `staff-form.mustache`를 등록/수정 재사용 가능한 구조로 정리
7. Controller/Service 테스트 작성 및 검증
