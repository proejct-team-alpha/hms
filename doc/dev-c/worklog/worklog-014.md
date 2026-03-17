# workflow-014 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/workflow/workflow-014.md` 요구사항 확인
- [x] 체크리스트를 구현 전에 먼저 출력

## 작업 목표
- `POST /admin/staff/deactivate` 직원 비활성화 처리
- 실제 삭제 없이 `Staff.active = false`로 변경
- 성공 시 직원 목록 리다이렉트 + 성공 메시지 표시
- 본인 비활성화 금지 / 이미 비활성화된 직원 재비활성화 금지 / 존재하지 않는 직원 차단
- 직원 목록 화면에서 비활성화 버튼 노출 규칙 반영

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffItemResponse.java`
- `src/main/resources/templates/admin/staff-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`

## 구현 내용
1. `AdminStaffController`
- `POST /admin/staff/deactivate` 추가
- 로그인한 관리자 username을 기준으로 서비스에 비활성화 요청 위임
- 성공 시 `successMessage` flash 설정 후 직원 목록으로 리다이렉트
- 비즈니스 예외 발생 시 `errorMessage` flash 설정 후 직원 목록으로 리다이렉트

2. `AdminStaffService`
- `deactivateStaff(Long staffId, String currentUsername)` 추가
- 대상 직원 조회 후 `active=false`로 변경
- 본인 계정 비활성화 금지 처리
- 이미 비활성화된 직원 재요청 금지 처리
- 존재하지 않는 직원은 not found 예외 처리
- 직원 목록 조회 시 현재 로그인 username을 받아 버튼 노출 가능 여부 계산

3. `AdminStaffItemResponse`
- 목록 화면 액션 제어용 필드 추가
  - `deactivatable`
  - `deactivateStatusLabel`
- 본인 행/이미 비활성화된 행을 템플릿에서 분기할 수 있도록 확장

4. `staff-list.mustache`
- 관리 컬럼에 수정 버튼과 비활성화 버튼을 함께 배치
- 비활성화 가능한 활성 직원에게만 POST form 버튼 노출
- 본인 행은 `본인` 라벨 표시
- 이미 비활성화된 직원은 `비활성` 라벨 표시
- 성공/실패 flash 메시지 영역 추가
- 기존 깨진 한글 문구도 함께 UTF-8 기준으로 정리

5. 테스트
- `AdminStaffControllerTest`
  - 목록 조회가 현재 로그인 username까지 서비스에 전달되는지 검증
  - 비활성화 성공 리다이렉트 검증
  - 비활성화 실패 리다이렉트 + 에러 메시지 검증
- `AdminStaffServiceTest`
  - 비활성화 성공 시 `active=false` 변경 검증
  - 본인 비활성화 금지 검증
  - 이미 비활성화된 직원 재비활성화 금지 검증
  - 존재하지 않는 직원 예외 검증

## 검증 결과
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.staff.*'`
- 결과: `BUILD SUCCESSFUL`

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/workflow/workflow-014.md`

## 남은 TODO / 리스크
- 현재 비활성화는 역할별 추가 제약 없이 공통 처리하므로, 향후 `DOCTOR` 비활성화 시 예약/스케줄 연동 정책이 필요하면 별도 후속 작업이 필요함
- 목록 화면의 문구 깨짐이 일부 다른 staff 템플릿에도 남아 있을 수 있어 후속 점검 여지는 있음
