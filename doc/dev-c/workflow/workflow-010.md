# 관리자 예약 취소 API 경로 정리 명세서 (workflow-010)

## 문제 정의
예약 취소 API를 관리자 영역 경로로 이동해 `/admin/api/reservations/{id}/cancel`로 변경하고,
별도 Security 규칙 추가 없이 기존 `/admin/**` 관리자 권한 정책을 그대로 활용한다.

## 대상 사용자
- 관리자(ROLE_ADMIN): 예약 취소 API 호출 가능

## 전달 요구사항 (우선순위)
1. [P0] 예약 취소 API 경로를 `/admin/api/reservations/{id}/cancel`로 변경
2. [P0] `SecurityConfig`에 추가했던 `/api/reservations/** -> ROLE_ADMIN` 규칙 제거
3. [P0] 기존 `/admin/** -> ROLE_ADMIN` 규칙으로 권한 제어
4. [P0] 응답 포맷은 `Resp<AdminReservationCancelResponse>` 유지
5. [P1] 관련 테스트 경로와 보안 기대값 갱신

## 시작 조건 & 전제
- 컨트롤러 내부 수동 권한 체크는 다시 넣지 않음
- 기존 서비스 로직(`adminReservationService.cancelReservation(id)`) 재사용
- DTO 구조(`AdminReservationCancelResponse`)는 유지

## 엣지 케이스 & 에러 시나리오
- 존재하지 않는 예약 ID: 404
- 취소 불가 상태(`COMPLETED`, `CANCELLED`): 409
- 비관리자 접근: `/admin/**` 보안 정책으로 차단
- 미인증 접근: 인증 정책에 따른 차단

## 범위 밖 (명시적 제외)
- 응답 DTO 구조 변경
- SSR 취소 기능 변경
- 예약 취소 비즈니스 규칙 변경

## 수용 기준
- [ ] 엔드포인트가 `/admin/api/reservations/{id}/cancel`로 동작한다.
- [ ] `SecurityConfig`의 별도 `/api/reservations/**` 규칙이 제거된다.
- [ ] `/admin/**` 규칙만으로 관리자 권한이 적용된다.
- [ ] API 테스트가 새 경로 기준으로 통과한다.

## 구현 순서
1. `AdminReservationApiController`의 `@RequestMapping` 경로 변경
2. `SecurityConfig`에서 `/api/reservations/**` 규칙 제거
3. API Controller 테스트 요청 경로 수정
4. 보안/에러 응답 테스트 기대값 점검
5. 예약 모듈 테스트 실행 및 회귀 확인