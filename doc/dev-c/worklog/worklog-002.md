# worklog-002

## 작업 요청
- `doc/dev-c/workflow/workflow-002.md` 명세 실행
- 작업 전 문서 확인 + 준수 체크리스트 출력 후 구현
- 결과를 `doc/dev-c/worklog/worklog-002.md`에 기록

## 사전 확인 문서
- `AGENTS.md`
- `.ai/memory.md`
- `doc/PROJECT_STRUCTURE.md`
- `doc/RULE.md`
- `doc/dev-c/workflow/workflow-002.md`

## 준수 체크리스트
- [x] 로컬 규칙 문서 우선 적용
- [x] admin 모듈 경계 내 변경
- [x] JS 규칙(`const`, `async/await`, 명시적 에러 처리) 적용
- [x] 화면 구조(카드/그리드 레이아웃) 유지
- [x] API 실패 메시지 표시 구현
- [x] 60초 자동 새로고침 구현

## 변경 파일 목록
- `src/main/resources/templates/admin/dashboard.mustache`
- `src/main/resources/static/js/page/admin-dashboard.js` (신규)

## 구현 내용
1. 템플릿 하드코딩 차트 제거
- 기존 mock bar/재고 그래프 마크업 제거
- 두 차트 영역을 `canvas` 기반으로 변경
  - `#daily-patient-canvas`
  - `#category-stock-canvas`
- 실패 메시지 영역 추가
  - `#daily-patient-error`
  - `#category-stock-error`

2. JS 동적 렌더링 추가
- `/admin/dashboard/chart` API 호출
- 응답(`Resp.body`) 파싱 후 canvas 렌더링
- 일별 환자수 막대 차트 렌더링
- 카테고리별 재고 수평 막대 차트 렌더링
- 빈 데이터 시 `데이터 없음` 표시

3. 실패/갱신 처리
- API 실패 시 차트 clear + 실패 메시지 노출
- `isFetching` 플래그로 중복 요청 방지
- `setInterval` 60초 자동 새로고침 적용

## 검증
- 정적 검증: 템플릿 스크립트 경로와 DOM id 연결 확인
- 런타임 테스트: 미실행

## 리스크 / TODO
- 실제 브라우저에서 canvas 크기/레이블 가독성 수동 확인 필요
- 차트 스타일(색상, 폰트, 축 눈금) 세부 조정 가능
