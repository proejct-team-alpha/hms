# Admin 라우트 도메인 분리 명세서 (workflow-004)

## 문제 정의
현재 관리자 화면 라우팅이 `AdminPageController` 한 곳에 몰려 있어 도메인 구조와 맞지 않는다.
`reservation / reception`부터 점진적으로 도메인별 컨트롤러로 분리한다.

## 대상 사용자
- 개발자: admin 모듈 구조 정리 및 유지보수성 개선
- 관리자: 기존 메뉴에서 동일하게 화면 접근 가능해야 함

## 핵심 요구사항 (우선순위순)
1. [P0] 점진적 분리 방식 적용 (`AdminPageController` 즉시 제거하지 않음)
2. [P0] 1차 분리 범위는 `reservation / reception`
3. [P0] `AdminReservationController`에서 예약 목록 라우트 담당
4. [P0] `AdminReceptionController` 신설 후 `/admin/reception/list` 접근 가능하게 구성
5. [P0] `reception`은 신규 템플릿 생성 없이 기존 `admin/reservation-list` 재사용
6. [P1] 데이터 로직(Service/Repository)은 이번 단계에서 추가하지 않음

## 제약 조건 & 전제
- URL prefix는 `/admin/**` 유지
- 도메인별 패키지 구조(`admin/reservation`, `admin/reception`) 준수
- 기존 화면 동작/레이아웃 깨짐 없이 라우트만 분리

## 엣지 케이스 & 에러 시나리오
- 동일 URL이 `AdminPageController`와 신규 도메인 컨트롤러에 중복 매핑될 위험
- 사이드바 링크와 실제 컨트롤러 경로 불일치 가능
- `reception` 경로는 생기지만 템플릿 재사용으로 화면 문구는 "예약" 기준일 수 있음

## 범위 밖 (명시적 제외)
- reception 전용 템플릿 신규 제작
- reception 전용 조회 로직/필터/페이징 구현
- `department/rule/staff/item` 라우트의 도메인 분리

## 수용 기준
- [ ] `/admin/reservation/list`가 `AdminReservationController`에서 열림
- [ ] `/admin/reception/list`가 `AdminReceptionController`에서 열림
- [ ] `/admin/reception/list` 접속 시 `admin/reservation-list` 템플릿이 렌더링됨
- [ ] `AdminPageController`는 1차 범위 밖 라우트만 유지되어 충돌 없음

## 구현 순서
1. `AdminPageController`의 reservation/reception 관련 매핑 존재 여부 진단
2. `admin/reservation` 패키지에 `AdminReservationController` 생성 및 `/admin/reservation/list` 매핑 이관
3. `admin/reception` 패키지에 `AdminReceptionController` 생성 및 `/admin/reception/list` 매핑 추가
4. reception 라우트는 1차에서 `admin/reservation-list` 템플릿 재사용
5. `AdminPageController`에서 중복 매핑 제거
6. 메뉴 링크와 라우트 정합성 확인
7. 테스트 실행으로 매핑 충돌/부팅 오류 검증
