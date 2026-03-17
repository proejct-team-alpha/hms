# 관리자 예약 취소 API 리팩토링 명세서 (workflow-009)

## 문제 정의
`AdminReservationApiController`의 응답 구조와 권한 처리 방식을 리팩토링한다.
응답은 팀 공통 포맷(`Resp`)으로 통일하고, 권한 검증은 Security 설정으로 이관한다.

## 대상 사용자
- 관리자(ROLE_ADMIN): 예약 취소 API 호출 가능

## 전달 요구사항 (우선순위)
1. [P0] API 성공 응답을 `Resp.ok(...)` 형식으로 통일
2. [P0] 취소 결과 DTO를 팀 네이밍 규칙에 맞게 정리
   - `AdminReservationCancelResponse` (`reservationId`, `status`)
3. [P0] 기존 전용 응답 래퍼 DTO(`success/data/message`) 정리
4. [P0] 컨트롤러 내부 권한 검증 로직 제거
5. [P0] `/api/reservations/**` 접근 권한을 Security 설정에서 `ROLE_ADMIN`으로 제한
6. [P1] Controller 테스트를 응답/권한 변경 사항에 맞게 수정

## 시작 조건 & 전제
- API 경로 유지: `POST /api/reservations/{id}/cancel`
- 기존 취소 비즈니스 규칙 유지 (`RESERVED`, `RECEIVED`만 취소 가능)
- 서비스 로직 재사용: `adminReservationService.cancelReservation(id)`
- 예외 처리 포맷은 `GlobalExceptionHandler` 규칙 유지

## 엣지 케이스 & 에러 시나리오
- 존재하지 않는 예약 ID: 404
- 취소 불가 상태(`COMPLETED`, `CANCELLED`): 409
- 비관리자 접근: 403 (Security 차단)
- 미인증 접근: 인증 정책에 따른 차단

## 범위 밖 (명시적 제외)
- SSR 취소 기능(`/admin/reservation/cancel`) 변경
- 예약 도메인 상태 전이 규칙 변경
- 취소 외 신규 예약 API 추가

## 수용 기준
- [ ] `POST /api/reservations/{id}/cancel` 성공 응답이 `Resp<AdminReservationCancelResponse>`다.
- [ ] `AdminReservationCancelResponse`에 `reservationId`, `status`가 포함된다.
- [ ] 컨트롤러 수동 권한 체크 메서드가 제거된다.
- [ ] Security 설정에 `/api/reservations/** -> ROLE_ADMIN`이 반영된다.
- [ ] 리팩토링 후 테스트가 통과한다.

## 구현 순서
1. 기존 `AdminReservationApiController` 응답 구조를 `Resp.ok(...)`로 변경
2. DTO 정리 (`AdminReservationCancelResponse` 단일 결과 DTO로 정렬)
3. 불필요한 응답 DTO 제거
4. 컨트롤러 권한 체크 로직 삭제
5. `SecurityConfig`에 `/api/reservations/**` 권한 정책 추가
6. API Controller 테스트 갱신
7. 예약 모듈 테스트 실행 및 회귀 확인