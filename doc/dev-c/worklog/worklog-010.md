# workflow-010 작업 로그

## 작업 전 준수 항목 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/workflow/workflow-010.md` 요구사항 확인
- [x] 체크리스트를 구현 전에 먼저 출력

## 작업 목표
- 예약 취소 API 경로를 `/admin/api/reservations/{id}/cancel`로 변경
- `SecurityConfig`의 `/api/reservations/** -> ROLE_ADMIN` 규칙 제거
- 기존 `/admin/** -> ROLE_ADMIN` 규칙으로 관리자 권한 유지
- `Resp<AdminReservationCancelResponse>` 응답 구조 유지
- 관련 테스트 갱신

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationApiController.java`
- `src/main/java/com/smartclinic/hms/config/SecurityConfig.java`
- `src/test/java/com/smartclinic/hms/admin/reservation/AdminReservationApiControllerTest.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java`

## 구현 내용
1. `AdminReservationApiController`
- 클래스 매핑을 `/api/reservations`에서 `/admin/api/reservations`로 변경
- 취소 API 엔드포인트를 관리자 영역 URL로 정리

2. `SecurityConfig`
- `/api/reservations/**` 전용 관리자 권한 규칙 제거
- 기존 `/admin/**` 보안 규칙으로 권한 검사를 일원화

3. `AdminReservationApiControllerTest`
- 요청 경로를 `/admin/api/reservations/{id}/cancel` 기준으로 수정
- 테스트 보안 설정도 `/admin/**` 기준으로 정리

4. `AdminReservationController`
- 예약 목록 SSR 테스트 검증 중 Mustache 렌더링에서 `model` 바인딩 누락 이슈 확인
- 컨트롤러는 `HttpServletRequest`만 유지하고 `ModelAndView`는 사용하지 않도록 정리
- 원인은 `@WebMvcTest` 환경에서 request attribute 노출 설정이 빠진 테스트 설정이었고, 테스트에 Mustache request attribute 노출 옵션을 명시해 해결
- 템플릿이 기대하는 `{{model...}}` 구조를 유지하면서 테스트와 실제 렌더링이 모두 동작하도록 정리

## 검증 결과
- 실행 명령어: `./gradlew test --tests 'com.smartclinic.hms.admin.reservation.*'`
- 결과: `BUILD SUCCESSFUL`

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/workflow/workflow-010.md`

## 남은 TODO / 리스크
- `AdminDashboardController`도 동일하게 `HttpServletRequest`만 사용 중이므로, 추후 Mustache 렌더링 안정성 관점에서 동일 패턴 점검이 필요할 수 있음
