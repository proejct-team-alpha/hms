# 직원 등록/수정 화면의 의사 전용 정보 구조 정리 명세서 (workflow-032)

## 문제 정의
직원 등록/수정 화면에서 `부서`와 `전문분야` 입력 방식이 현재 엔티티 책임과 어긋나 있었다. `Staff`는 공통 직원 정보만 가지도록 두고, 의사 정보(`Doctor`)에서만 부서를 관리하도록 화면과 서비스 구조를 정리한다.

## 대상 화면
- `GET /admin/staff/new`
- `GET /admin/staff/detail?staffId={id}`

## 현재 판단
- 공통 정보에 `부서`가 있으면 `Staff`와 `Doctor`가 같은 책임을 중복으로 가지게 된다.
- `전문분야`를 자유입력으로 두면 실제 존재하지 않는 문자열이 저장될 수 있다.
- `재직 상태`는 직원 공통 정보이므로, 별도 큰 섹션보다 공통 정보 안의 작은 필드로 두는 편이 더 자연스럽다.
- 등록 화면과 수정 화면이 서로 다른 규칙을 가지면 유지보수 비용이 커지므로 같은 패턴으로 맞추는 것이 안전하다.

## 확정 방향
1. 공통 직원 정보 영역에서 `부서` 입력을 제거한다.
2. `부서`는 의사 전용 정보 영역에서만 선택한다.
3. 의사 전용 정보의 `전문분야` 자유입력 필드는 제거한다.
4. 의사 생성/수정 시 선택한 부서를 기준으로 `Doctor.department`, `Doctor.specialty`를 함께 저장한다.
5. `재직 상태`는 공통 정보 영역의 작은 select 필드로 둔다.
6. 역할이 `DOCTOR`가 아닐 때는 기존처럼 의사 전용 정보 영역을 숨긴다.
7. 직원 목록 등 조회 화면도 의사 부서는 `Doctor.department`를 기준으로 보이도록 맞춘다.

## 구현 범위
### 1. 화면(UI)
- 공통 직원 정보
  - 이름
  - 로그인 아이디
  - 비밀번호
  - 사번
  - 역할
  - 재직 상태
- 의사 전용 정보
  - 부서 select
  - 진료 가능 요일
- 제거 대상
  - 공통 정보의 부서
  - 의사 전용 정보의 전문분야 자유입력

### 2. 요청/검증
- `departmentId`는 공통 필수가 아니라 `DOCTOR`일 때만 의미를 가진다.
- `DOCTOR`인데 `departmentId`가 없으면 same-view 검증 에러로 처리한다.
- 비의사 역할에서는 `departmentId`가 들어와도 저장 로직에 영향 주지 않도록 한다.
- admin API 계약에서도 `specialty`를 제거한다.

### 3. 서비스 로직
- 생성과 수정 모두 `Staff.department`는 저장하지 않는다.
- 의사 정보는 `Doctor.department`만 기준으로 관리한다.
- `Doctor.specialty`는 자유입력 대신 선택한 부서명 기준으로 저장한다.
- 직원 목록 조회는 의사 부서를 `Doctor.department`에서 읽도록 맞춘다.

### 4. 테스트
- 컨트롤러 테스트
  - 등록/수정 화면 렌더링
  - `departmentId` select 노출
  - `specialty` 제거
  - same-view 검증 실패 시 부서 에러 유지
- 서비스 테스트
  - 의사 생성/수정 시 부서 기반 저장
  - 비의사 수정 시 부서 입력 무시
  - 직원 목록의 의사 부서 노출
- API 테스트
  - 수정 응답에서 `specialty` 제거 확인

## 수용 기준
- [x] 직원 등록 화면 공통 정보에서 부서 입력이 제거된다.
- [x] 직원 수정 화면 공통 정보에서 부서 입력이 제거된다.
- [x] 재직 상태가 공통 정보의 작은 필드로 자연스럽게 배치된다.
- [x] 의사 전용 정보에서만 부서 select가 보인다.
- [x] 전문분야 자유입력 필드가 제거된다.
- [x] `DOCTOR` 등록/수정 시 선택한 부서를 기준으로 의사 정보가 저장된다.
- [x] `admin.staff` 범위 테스트가 통과한다.
- [ ] 전체 `./gradlew test`가 통과한다.

## 구현 점검 결과
- 등록 화면과 수정 화면 모두 같은 `staff-form.mustache` 패턴으로 정리했다.
- `CreateAdminStaffRequest`, `UpdateAdminStaffRequest`, admin API 요청/응답 DTO에서 `specialty`를 제거했다.
- 수정 서비스는 `Staff.update(..., null, active)`를 사용해 공통 직원 엔티티에 부서가 남지 않도록 맞췄다.
- 직원 목록 조회 쿼리는 `Doctor.department` 기준으로 의사 부서를 표시하도록 보정했다.
- `AdminStaffControllerTest`, `AdminStaffServiceTest`, `AdminStaffApiControllerTest`는 새 계약 기준으로 갱신했다.

## 검증 결과
- `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'`
  - `BUILD SUCCESSFUL`
- `./gradlew test`
  - `BUILD FAILED`
  - 실패 테스트
    - `AdminDepartmentControllerTest > list renders empty state`
    - `ReservationTest > cancel — COMPLETED면 IllegalStateException`

## 리뷰 포인트
- 부서를 `Staff` 공통 정보가 아니라 `Doctor` 전용 책임으로 정리한 방향이 현재 도메인 규칙과 잘 맞는지 확인이 필요하다.
- `Doctor.specialty`를 자유입력이 아니라 부서명 기준으로 유지한 판단이 이후 운영 규칙과 충돌하지 않는지 봐주면 좋다.
- 전체 테스트 실패 2건은 `admin.staff` 변경 범위 밖이라, 별도 정리 후 최종 완료 체크를 닫는 것이 안전하다.