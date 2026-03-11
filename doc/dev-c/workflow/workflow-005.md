# Admin 컨트롤러 정리 명세서 (workflow-005)

## 문제 정의
`AdminReceptionController`는 프로젝트에서 실사용 가능성이 낮아 제거하고 싶다.
`AdminPageController`도 팀 규칙(도메인별 컨트롤러 분리)에 맞지 않아 삭제하고 싶다.

## 대상 사용자
- 개발자(관리자 모듈 유지보수 담당)

## 핵심 요구사항 (우선순위순)
1. [P0] `AdminReceptionController` 삭제
2. [P0] `AdminPageController` 삭제
3. [P0] `AdminPageController`가 담당하던 `department/rule/staff/item` 화면 라우트는 각 도메인 컨트롤러로 이관 후 유지
4. [P1] 사이드바 링크가 이관된 라우트와 정확히 일치하도록 정리
5. [P1] 매핑 충돌/누락 없이 부팅 및 테스트 통과

## 제약 조건 & 전제
- 모듈 구조 규칙 준수: 도메인별 컨트롤러로 분리
- URL prefix는 `/admin/**` 유지
- 기능 삭제가 아니라 라우트 소유권 이동이 목적(`department/rule/staff/item`)

## 엣지 케이스 & 에러 시나리오
- 컨트롤러 삭제 후 링크가 404 되는 경우
- 기존 매핑과 신규 매핑 중복으로 인한 충돌
- 템플릿 경로 오타로 화면 렌더 실패

## 범위 밖 (명시적 제외)
- 신규 비즈니스 로직(Service/Repository) 추가
- 화면 디자인 변경
- dashboard/api 로직 변경

## 수용 기준
- [ ] `AdminReceptionController` 파일이 제거된다.
- [ ] `AdminPageController` 파일이 제거된다.
- [ ] `department/rule/staff/item` 라우트가 각 도메인 컨트롤러에서 정상 동작한다.
- [ ] 사이드바 메뉴 클릭 시 모두 정상 이동한다.
- [ ] 테스트(최소 `./gradlew test`) 통과한다.

## 구현 순서
1. `AdminPageController`가 가진 라우트를 분류(`department/rule/staff/item`)
2. 각 도메인 패키지에 컨트롤러 생성 또는 기존 컨트롤러에 GET 매핑 이관
3. 사이드바 링크가 이관된 URL과 일치하는지 정합성 점검
4. `AdminReceptionController` 삭제
5. `AdminPageController` 삭제
6. 매핑 충돌/404 여부 수동 점검
7. 테스트 실행으로 회귀 확인
