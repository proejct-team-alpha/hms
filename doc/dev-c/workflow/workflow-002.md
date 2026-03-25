# Admin Dashboard Chart 동적 렌더링 명세서 (workflow-002)

## 문제 정의
`admin/dashboard.mustache`의 canvas 차트 렌더링 데이터가 하드코딩되어 있어 유지보수와 정확성이 떨어진다.
`AdminDashboardApiController`의 `/chart` 데이터를 `admin-dashboard.js`에서 받아 동적으로 렌더링하도록 변경한다.

## 대상 사용자
- 관리자(ROLE_ADMIN): 대시보드 차트를 통해 운영 현황 확인
- 개발자: 하드코딩 제거 및 데이터/표현 분리 유지보수

## 핵심 요구사항 (우선순위순)
1. [P0] 차트 하드코딩 데이터 제거
2. [P0] `AdminDashboardApiController`의 `/chart` API 응답을 `resources/js/page/admin-dashboard.js`에서 조회해 canvas 렌더링
3. [P0] 기존 화면 구조/레이아웃 깨짐 없이 적용
4. [P0] API 실패 시 차트 영역에 실패 메시지 표시
5. [P1] 주기적 자동 새로고침 적용 (60초 주기)

## 제약 조건 & 전제
- SSR 템플릿 구조(`dashboard.mustache`) 유지
- JS 규칙 준수(`const` 우선, async/await 중심, 명시적 에러 처리)
- API 경로/응답 포맷과 JS 파싱 구조 일치 필요

## 엣지 케이스 & 에러 시나리오
- API 응답이 빈 배열 또는 null인 경우
- 네트워크 실패/타임아웃/HTTP 오류
- 자동 새로고침 중 이전 요청이 진행 중인 경우(중복 렌더링/깜빡임 방지)
- 캔버스 DOM이 없는 상태에서 스크립트 실행되는 경우

## 범위 밖 (명시적 제외)
- 차트 디자인 전면 개편
- 대시보드 전체 API 스펙 변경
- WebSocket 기반 실시간 푸시 도입

## 수용 기준
- [ ] 하드코딩 차트 데이터가 제거된다.
- [ ] `/chart` API 기반으로 canvas가 정상 렌더링된다.
- [ ] API 실패 시 사용자 메시지가 표시된다.
- [ ] 60초 자동 새로고침이 동작한다.
- [ ] 기존 화면 레이아웃이 유지된다.

## 구현 순서
1. `dashboard.mustache`에서 차트 하드코딩 데이터 제거 및 canvas/메시지 영역 식별자 정리
2. `admin-dashboard.js`에 `/chart` API 호출 함수 추가 (async/await + HTTP 실패 처리)
3. API 응답을 canvas 렌더링 데이터로 매핑
4. 실패 시 차트 영역에 "데이터를 불러오지 못했습니다" 메시지 표시
5. `setInterval` 기반 60초 자동 새로고침 적용
6. 중복 요청 방지(요청 중 재호출 제어) 로직 적용
7. 화면 구조(레이아웃/DOM) 비회귀 확인
8. 관련 테스트 또는 수동 검증 시나리오 정리
