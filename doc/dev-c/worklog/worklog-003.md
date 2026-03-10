# worklog-003

## 작업 요청
- `doc/dev-c/workflow/workflow-003.md` 실행
- 작업 전 문서 확인 + 준수 체크리스트 출력
- 진단 후 수정(연결만), 데이터 로직은 2차 분리

## 사전 확인 문서
- `AGENTS.md`
- `.ai/memory.md`
- `doc/PROJECT_STRUCTURE.md`
- `doc/RULE.md`
- `doc/dev-c/workflow/workflow-003.md`

## 준수 체크리스트
- [x] 로컬 규칙 문서 우선 적용
- [x] 변경 범위는 admin 메뉴/라우트/템플릿 연결만
- [x] Service/Repository 데이터 로직 미추가
- [x] URL prefix `/admin/**` 유지
- [x] 기존 L2/S4 레이아웃 패턴 유지

## 진단 결과
1. `sidebar-admin.mustache`의 예약 링크가 `/admin/reservation-list`로 되어 있어 목표 경로(`/admin/reservation/list`)와 불일치
2. `/admin/reception/list` 링크/컨트롤러/템플릿이 없어 진입 불가
3. 따라서 "메뉴 클릭 → URL 진입 → 화면 렌더링" 연결 조건 미충족

## 변경 파일 목록
- `src/main/resources/templates/common/sidebar-admin.mustache`
- `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationController.java` (신규)
- `src/main/java/com/smartclinic/hms/admin/reception/AdminReceptionController.java` (신규)
- `src/main/resources/templates/admin/reception-list.mustache` (신규)

## 구현 내용
1. 관리자 사이드바 링크 정렬
- `/admin/reservation-list` → `/admin/reservation/list` 수정
- `전체 접수 목록` 메뉴 추가: `/admin/reception/list`

2. 라우트 연결 추가 (연결만)
- `GET /admin/reservation/list` → `admin/reservation-list`
- `GET /admin/reception/list` → `admin/reception-list`

3. 접수 목록 placeholder 화면 추가
- `admin/reception-list.mustache` 생성
- 내용: 제목 + "데이터 로직은 2차 작업" 안내 문구

## 검증 결과
- 실행 테스트:
  - `./gradlew test --tests "com.smartclinic.hms.admin.dashboard.AdminDashboardControllerTest" --tests "com.smartclinic.hms.admin.dashboard.AdminDashboardApiControllerTest"`
- 결과: `BUILD SUCCESSFUL`

## 범위 밖 (의도적으로 미반영)
- `/admin/reception/list` 실제 목록 조회 로직
- 접수 상태 필터/페이징/검색
- Service/Repository 추가 구현

## TODO / 리스크
- 2차에서 `AdminReceptionService/Repository` 연결 필요
- 사이드바의 다른 URL(`department-list`, `rule-list`, `staff-list`, `item-list`)도 계층형 표준(`/admin/{resource}/list`)으로 일괄 정리 여부 결정 필요
