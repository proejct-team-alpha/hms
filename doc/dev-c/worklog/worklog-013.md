# worklog-013

## 사전 확인 체크리스트
- [x] `AGENTS.md` 읽음
- [x] `.ai/memory.md` 읽음
- [x] `doc/PROJECT_STRUCTURE.md` 읽음
- [x] `doc/RULE.md` 읽음
- [x] `doc/dev-c/workflow/workflow-013.md` 요구사항 확인
- [x] 구현 전 준수 항목 체크리스트 먼저 출력함

## 구현 내용
1. 직원 수정 SSR 흐름 추가
- `GET /admin/staff/detail?staffId={id}` 추가
- `POST /admin/staff/update` 추가
- 수정 성공 시 직원 목록으로 리다이렉트 + `successMessage` 플래시 처리

2. 직원 수정 서비스 구현
- 이름, 부서 수정 지원
- 비밀번호는 입력된 경우에만 BCrypt로 갱신
- `DOCTOR` 역할인 경우 전문 분야, 진료 가능 요일 수정 지원
- `DOCTOR`인데 연결된 `Doctor` 엔티티가 없으면 예외 처리

3. 등록/수정 폼 통합 정리
- `staff-form.mustache`를 등록/수정 겸용으로 재구성
- 수정 화면에서는 로그인 아이디, 사번, 역할을 읽기 전용 표시
- 수정 화면에서만 의사 전용 섹션(전문 분야, 진료 가능 요일) 노출
- 재직 상태는 수정 화면 일반 수정 범위에서 제외 유지

4. 목록 화면 수정 링크 정리
- 직원 목록의 수정 버튼이 `/admin/staff/detail?staffId={id}`로 이동하도록 변경

5. 의사 정보 수정 지원 보강
- `Doctor` 엔티티에 프로필 수정 메서드 추가
- `DoctorRepository`에 `findByStaffId` 조회 추가

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffFormResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffItemResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/UpdateAdminStaffRequest.java`
- `src/main/java/com/smartclinic/hms/doctor/DoctorRepository.java`
- `src/main/java/com/smartclinic/hms/domain/Doctor.java`
- `src/main/resources/templates/admin/staff-form.mustache`
- `src/main/resources/templates/admin/staff-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`

## 검증 결과
- 실행 명령: `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'`
- 결과: `BUILD SUCCESSFUL`

## 참조 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 작업 명세: `doc/dev-c/workflow/workflow-013.md`

## 남은 TODO / 리스크
- 재직 상태 비활성화는 별도 기능으로 아직 분리 구현되지 않음
- `DOCTOR` 신규 등록 시 `Doctor` 엔티티 생성 흐름은 이번 범위에 포함하지 않음
- 직원 등록/수정 폼 공통화가 진행됐지만, 향후 전용 ViewModel 분리 여부는 한 번 더 검토 가능
