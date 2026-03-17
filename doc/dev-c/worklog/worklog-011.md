# workflow-011 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/workflow/workflow-011.md` 요구사항 확인
- [x] 체크리스트를 구현 전에 먼저 출력

## 작업 목표
- `GET /admin/staff/list` 직원 목록 SSR 구현
- DB 연동 기반 직원 목록 조회
- 키워드 검색(`이름`, `로그인 아이디`) 지원
- 역할 필터, 재직 상태 필터 지원
- 페이징 지원

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffRepository.java`
- `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffFilterOptionResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffItemResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffListResponse.java`
- `src/main/java/com/smartclinic/hms/admin/staff/dto/AdminStaffPageLinkResponse.java`
- `src/main/resources/templates/admin/staff-list.mustache`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffControllerTest.java`
- `src/test/java/com/smartclinic/hms/admin/staff/AdminStaffServiceTest.java`

## 구현 내용
1. `AdminStaffRepository`
- 직원 목록 조회용 Projection + `@Query` 페이지 조회 메서드 추가
- 검색 조건: 이름/로그인 아이디 키워드
- 필터 조건: 역할, 재직 상태(`active`)
- 기존 `countByActiveTrue()`는 유지하여 대시보드 통계 영향 없도록 처리

2. `AdminStaffService`
- 직원 목록 조회 서비스 추가
- 기본 페이지/사이즈 보정 로직 추가
- 역할 문자열 파라미터를 `StaffRole`로 해석하는 로직 추가
- 재직 상태 파라미터(`ALL`, `ACTIVE`, `INACTIVE`) 해석 로직 추가
- SSR 렌더링용 목록/필터/페이지 링크 DTO 조합
- 역할 배지, 재직 상태 배지용 표시 값 생성

3. `AdminStaffController`
- `GET /admin/staff/list`에 검색/필터/페이징 파라미터 연결
- `HttpServletRequest`에 `model` 속성으로 목록 응답 DTO 바인딩
- SSR 뷰 `admin/staff-list` 반환

4. `staff-list.mustache`
- JS 더미 데이터 기반 렌더링 제거
- Mustache SSR 렌더링 구조로 교체
- 검색 입력, 역할 필터, 재직 상태 필터를 GET form으로 구성
- 목록 테이블과 페이징을 서버 응답 기준으로 렌더링

5. 테스트
- `AdminStaffServiceTest`
  - 키워드/역할/재직 상태 필터 조합 검증
  - 기본 페이징과 정렬 검증
- `AdminStaffControllerTest`
  - 기본 목록 렌더링 검증
  - 검색/필터 파라미터 전달 검증
  - `@WebMvcTest`에서 Mustache request attribute 노출 설정 명시

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
- 로컬: `doc/dev-c/workflow/workflow-011.md`

## 남은 TODO / 리스크
- 현재 직원 목록 화면의 `수정` 버튼은 `/admin/staff/form` 링크만 연결되어 있으며, 상세 편집 기능은 이번 범위에 포함하지 않음
- 필터 옵션 문구(`비활성`)는 팀에서 `퇴사`, `휴직` 등 세분화된 상태 정의가 확정되면 함께 조정 필요
