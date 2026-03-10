# worklog-004

## 작업 요청
- `doc/dev-c/workflow/workflow-004.md` 실행
- 작업 전 `AGENTS.md/.ai/memory.md/doc/PROJECT_STRUCTURE.md/doc/RULE.md` 확인
- 준수 체크리스트 먼저 출력 후 구현

## 사전 확인 문서
- `AGENTS.md`
- `.ai/memory.md`
- `doc/PROJECT_STRUCTURE.md`
- `doc/RULE.md`
- `doc/dev-c/workflow/workflow-004.md`

## 준수 체크리스트
- [x] 로컬 규칙 문서 우선 적용
- [x] 1차 분리 범위는 `reservation / reception`만 적용
- [x] `/admin/**` URL prefix 유지
- [x] reception은 신규 템플릿 생성 없이 `admin/reservation-list` 재사용
- [x] Service/Repository 데이터 로직 미추가
- [x] 매핑 충돌 검증을 위한 테스트 실행

## 변경 파일 목록
- `src/main/java/com/smartclinic/hms/admin/AdminPageController.java`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java` (신규)
- `src/main/java/com/smartclinic/hms/admin/reception/AdminReceptionController.java` (신규)
- `src/main/resources/templates/common/sidebar-admin.mustache`

## 구현 내용 요약
1. `AdminPageController`에서 reservation 매핑 제거
- 제거: `GET /admin/reservation-list`
- 유지: department/rule/staff/item 관련 기존 매핑

2. 도메인 컨트롤러 분리
- `AdminReservationController` 추가
  - `GET /admin/reservation/list` → `admin/reservation-list`
- `AdminReceptionController` 추가
  - `GET /admin/reception/list` → `admin/reservation-list` (1차 화면 재사용)

3. 사이드바 링크 정합성 반영
- `전체 예약 목록`: `/admin/reservation-list` → `/admin/reservation/list`
- `전체 접수 목록`: `/admin/reception/list` 신규 추가

## 검증 결과
- 실행 명령: `./gradlew test`
- 결과: `BUILD SUCCESSFUL`

## 참조 문서
- 로컬: `AGENTS.md`, `.ai/memory.md`, `doc/PROJECT_STRUCTURE.md`, `doc/RULE.md`
- 워크플로: `doc/dev-c/workflow/workflow-004.md`

## 남은 TODO / 리스크
- `AdminPageController`의 나머지 라우트(`department/rule/staff/item`)도 후속 단계에서 도메인별 컨트롤러로 점진 이관 필요
- `reception` 전용 템플릿/조회 로직은 2차 작업에서 분리 구현 필요
