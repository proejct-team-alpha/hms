# 관리자 예약 취소 API 구현 명세서 (workflow-008)

## 문제 정의
관리자 예약 관리에서 SSR 취소 기능은 유지하고, 별도로 API 전용 엔드포인트를 추가해 JSON 기반으로 예약 취소를 처리한다.

## 대상 사용자
- 관리자(ROLE_ADMIN): 예약 취소 API 호출 가능

## 전달 요구사항 (우선순위)
1. [P0] API 전용 컨트롤러 `AdminReservationApiController` 생성
2. [P0] `POST /api/reservations/{id}/cancel` 엔드포인트 제공
3. [P0] 기존 상태 전이 규칙 유지 (`RESERVED`, `RECEIVED`만 취소 가능)
4. [P0] 성공 시 JSON 응답
   - `success: true`
   - `data.reservationId`
   - `data.status: "CANCELLED"`
   - `message: "예약이 취소되었습니다."`
5. [P0] 기존 SSR 취소(`POST /admin/reservation/cancel`)는 유지
6. [P1] API 호출 권한은 `ROLE_ADMIN`으로 제한

## 시작 조건 & 전제
- URL prefix 및 모듈 구조 규칙 준수
- 서비스 트랜잭션/예외 처리 규칙(`CustomException`, `GlobalExceptionHandler`) 유지
- 기존 도메인 취소 로직(`reservation.cancel()`) 재사용

## 엣지 케이스 & 에러 시나리오
- 존재하지 않는 예약 ID: 404
- 취소 불가 상태(`COMPLETED`, `CANCELLED`): 409
- 관리자 권한 없는 호출: 403
- 인증 없는 호출: 보안 정책에 따른 차단

## 범위 밖 (명시적 제외)
- 기존 SSR 취소 화면/버튼 제거
- 상태 전이 규칙 변경
- 예약 취소 외 상태 변경 API 추가

## 수용 기준
- [ ] `POST /api/reservations/{id}/cancel`가 관리자 권한에서만 성공한다.
- [ ] 성공 응답 JSON 구조/값이 합의 내용과 일치한다.
- [ ] 취소 불가 상태 요청은 409를 반환한다.
- [ ] 존재하지 않는 예약 요청은 404를 반환한다.
- [ ] 기존 SSR 취소 기능이 정상 동작한다.
- [ ] Controller/Service 테스트가 추가되고 통과한다.

## 구현 순서
1. `AdminReservationApiController` 추가 및 라우트 선언
2. 서비스 취소 로직 재사용 가능한 응답 DTO 설계
3. API 성공 응답 포맷(`success/data/message`) 적용
4. 예외 매핑(404/409) 검증
5. 보안 권한(`ROLE_ADMIN`) 접근 제어 확인
6. Controller/Service 테스트 추가
7. 회귀 테스트 실행