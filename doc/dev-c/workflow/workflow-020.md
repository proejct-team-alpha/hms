# workflow-020

## 목적
- `AdminStaffServiceTest`, `AdminDepartmentServiceTest`를 중심으로 서비스 단위 테스트를 보강한다.
- 기존 테스트 자산은 유지하되, 구조를 조금 정리하면서 핵심 CRUD 시나리오와 회귀 방지 범위를 넓힌다.
- 비밀번호 암호화, 중복 방지, 상태 변경 같은 핵심 비즈니스 규칙이 테스트로 보장되도록 만든다.

## 대상 범위
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`
- `src/test/java/com/smartclinic/hms/admin/department/AdminDepartmentServiceTest.java`

## 작업 방향
1. 기존 테스트 파일은 유지한다.
2. 테스트 이름, 배치, 시나리오 흐름은 읽기 쉽게 정리한다.
3. 컨트롤러 테스트가 아니라 서비스 단위의 비즈니스 규칙 검증에 집중한다.
4. Given-When-Then 구조를 지키고, 결정적인 테스트만 추가한다.

## 보강 시나리오

### 1. AdminStaffServiceTest
#### 생성(Create)
- 직원 생성 성공
- 비밀번호 BCrypt 암호화 저장 확인
- `DOCTOR` 생성 시 `Doctor` 엔티티 동시 생성 확인

#### 중복 방지
- `username` 중복 차단
- `employeeNumber` 중복 차단

#### 수정(Update)
- 이름/부서 수정
- 비밀번호 입력 시 변경
- 비밀번호 미입력 시 기존 비밀번호 유지
- `DOCTOR` 수정 시 전문 분야/진료 가능 요일 반영

#### 상태 변경
- 비활성화 성공
- 본인 계정 비활성화 차단
- 이미 비활성화된 직원 재요청 차단

### 2. AdminDepartmentServiceTest
#### 생성(Create)
- 진료과 생성 성공
- active 값 저장 확인

#### 중복 방지
- 진료과명 중복 차단

#### 수정(Update)
- 진료과명 수정 성공
- 공백 이름 차단
- 중복 이름 차단
- 없는 ID 차단

#### 상태 변경
- 비활성화 성공
- 이미 비활성화된 진료과 차단
- 활성화 성공
- 이미 활성화된 진료과 차단

## 제약 조건
- 현재 서비스 API와 도메인 구조를 기준으로 테스트를 작성한다.
- 시간, 랜덤, 외부 연동 의존성 없이 결정적 테스트로 유지한다.
- 컨트롤러/템플릿 레벨 검증은 이번 범위에서 제외한다.
- 인코딩 표시 이슈가 있더라도 실제 assertion과 테스트 의도는 안정적으로 유지한다.

## 수용 기준
- [ ] `AdminStaffServiceTest`에 생성/수정/중복 방지/암호화/비활성화 핵심 시나리오가 보강된다.
- [ ] `AdminDepartmentServiceTest`에 생성/수정/중복 방지/상태 전이 핵심 시나리오가 보강된다.
- [ ] 두 테스트 파일이 기존보다 읽기 쉬운 구조로 정리된다.
- [ ] `admin.staff`, `admin.department` 범위 테스트가 통과한다.