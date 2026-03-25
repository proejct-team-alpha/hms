# Admin 예약 목록 구현 명세서 (workflow-006)

## 문제 정의
관리자 예약 목록(S18)을 구현해야 하며, `GET /admin/reservation/list`에서 전체 예약 목록 조회가 가능해야 한다.
목록은 페이징과 상태 필터를 지원하고, 실제 DB 연동까지 포함한다.

## 대상 사용자
- 관리자(ROLE_ADMIN): 예약 목록 조회/운영
- 개발자: 컨트롤러-서비스-리포지토리-템플릿 연동 구현 및 유지보수

## 핵심 요구사항 (우선순위순)
1. [P0] `AdminReservationController`에 `GET /admin/reservation/list` 구현
2. [P0] DB 연동 기반 예약 목록 조회 구현(더미/하드코딩 금지)
3. [P0] 상태 필터 지원: `ALL`, `RESERVED`, `RECEIVED`, `COMPLETED`, `CANCELLED`
4. [P0] 페이징 지원
5. [P1] 기본 정렬: `reservationDate DESC`, `timeSlot DESC`
6. [P1] 필터 파라미터 `status` 사용, 누락/잘못된 값은 `ALL` 처리

## 제약 조건 & 전제
- URL prefix는 `/admin/**` 유지
- 팀 규칙에 맞는 계층 분리(Controller → Service → Repository)
- 기존 관리자 레이아웃/템플릿 구조 유지

## 엣지 케이스 & 에러 시나리오
- `status`가 비정상 문자열일 때 `ALL`로 fallback
- 예약 데이터 0건일 때 빈 목록/페이지 렌더링 정상 동작
- 페이지 번호 범위 초과 시 빈 페이지 처리

## 범위 밖 (명시적 제외)
- 예약 취소/상태 변경 POST 로직
- 접수 목록(`/admin/reception/list`) 별도 구현
- 검색어/기간/진료과 등 추가 필터

## 수용 기준
- [ ] `/admin/reservation/list` 진입 시 DB 기반 목록이 렌더링된다.
- [ ] `status` 필터 5개 값이 동작한다.
- [ ] 잘못된 `status`는 `ALL`로 처리된다.
- [ ] 기본 페이징: `page=1`, `size=10`이 적용된다.
- [ ] 기본 정렬: `reservationDate DESC`, `timeSlot DESC`가 적용된다.

## 구현 순서
1. `AdminReservationController`의 GET `/list` 요청 파라미터(`page`, `size`, `status`) 설계
2. 상태 필터 파라미터 파싱 및 invalid 값의 `ALL` fallback 처리
3. 서비스 계층에서 페이징/정렬/상태 조건 조합
4. 리포지토리 계층에 상태 조건별 조회(ALL 포함) 구현
5. 응답 DTO/모델을 통해 템플릿 렌더링 데이터 구성
6. `reservation-list.mustache`에서 목록/필터/페이지네이션 바인딩
7. 테스트로 필터/페이징/fallback 케이스 검증
