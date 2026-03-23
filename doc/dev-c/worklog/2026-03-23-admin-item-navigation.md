# 2026-03-23 Admin Item Navigation Worklog

## 작업 기준 체크리스트
- [x] `AGENTS.md` 확인
- [x] `.ai/memory.md` 확인
- [x] `doc/PROJECT_STRUCTURE.md` 확인
- [x] `doc/RULE.md` 확인
- [x] `doc/dev-c/.person/reports/admin-item-navigation/` 리포트 기준 통합 정리

## 작업 목적
- 관리자 물품 메뉴 진입 구조를 단순화해 사이드바 복잡도를 낮춘다.
- 물품 목록 화면을 물품 관리의 중심 진입점으로 정리한다.
- 물품 목록 상단 액션과 검색 UI를 예약 목록 패턴에 가깝게 맞춰 관리자 목록형 화면의 일관성을 높인다.

## 보고서 소스
- `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1729.md`
- `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1731-item-use-button-color.md`
- `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1735-item-use-button-red-tone.md`
- `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1743-item-list-search-alignment.md`
- `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1750-item-list-category-select.md`
- `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1812-item-list-searchbar-attached.md`

## 변경 파일
- `src/main/java/com/smartclinic/hms/admin/item/AdminItemController.java`
- `src/main/java/com/smartclinic/hms/admin/item/AdminItemService.java`
- `src/main/resources/templates/common/sidebar-admin.mustache`
- `src/main/resources/templates/admin/item-list.mustache`

## 구현 내용
1. 물품 메뉴 진입 구조 단순화
- 관리자 사이드바에서 `물품 등록`, `물품 출고` 직접 메뉴를 제거하고 `물품 목록`, `입출고 내역` 중심 구조로 정리했다.
- 물품 등록과 물품 출고는 `물품 목록` 화면 상단 버튼으로 진입하도록 바꿨다.
- `AdminItemController`에서 물품 등록/출고 화면 진입 시에도 사이드바 활성 메뉴가 `물품 목록`으로 보이도록 플래그를 보완했다.

2. 물품 목록 상단 액션 정리
- `item-list.mustache` 상단에 `물품 등록` 기본 버튼과 `물품 출고` 보조 버튼을 배치했다.
- `물품 출고` 버튼은 가시성 피드백을 반영해 최종적으로 `bg-red-500`, `hover:bg-red-600` 계열로 조정했다.

3. 물품 목록 검색 UI 정리
- 기존 카테고리 칩 기반 검색 영역을 `카테고리 select + 검색 input + 조회 + 초기화` 구조로 정리했다.
- 검색 조건은 `GET /admin/item/list` 기준으로 `category`, `keyword`를 함께 처리하도록 맞췄다.
- `AdminItemService`에 서버 측 keyword 필터를 추가해 카테고리와 검색어를 함께 적용하도록 정리했다.
- 초성 검색은 기존 사용감을 유지하기 위해 서비스와 템플릿 양쪽에서 계속 동작하도록 맞췄다.

4. 검색바 배치 최종 정리
- 물품 목록 검색바를 별도 카드가 아니라 테이블 카드 상단 내부로 이동시켰다.
- 최종 형태는 예약 목록과 유사한 `bg-slate-50 + border-b + 검색 / 카테고리 / 조회 / 초기화` 구조다.
- `초기화`는 링크형 보조 액션으로 두고, 검색 입력의 실시간 필터링은 유지했다.

## 검증 결과
- 실행 명령어:
  - `.\gradlew.bat compileJava`
  - `git diff -- src/main/java/com/smartclinic/hms/admin/item/AdminItemController.java src/main/java/com/smartclinic/hms/admin/item/AdminItemService.java src/main/resources/templates/admin/item-list.mustache`
  - `git diff -- src/main/resources/templates/admin/item-list.mustache`
- 결과:
  - `compileJava` 성공
  - 이후 UI 세부 조정 단계는 diff 확인 중심으로 검토

## 최신 상태 요약
- 사이드바 물품 메뉴는 단순화되었고, 물품 목록이 메인 진입점 역할을 하도록 정리되었다.
- 물품 출고 버튼은 시각적으로 더 잘 드러나는 빨간 톤으로 정리되었다.
- 물품 목록 검색 영역은 예약 목록 패턴에 맞춰 카드 상단 부착형 필터 바로 정리되었다.
- 카테고리와 검색어는 서버/화면 양쪽에서 함께 유지되며, 검색 UX는 조회형 구조와 실시간 필터링을 함께 가진 혼합형으로 정리되었다.

## 참고 문서
- 로컬: `AGENTS.md`
- 로컬: `.ai/memory.md`
- 로컬: `doc/PROJECT_STRUCTURE.md`
- 로컬: `doc/RULE.md`
- 로컬: `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1729.md`
- 로컬: `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1731-item-use-button-color.md`
- 로컬: `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1735-item-use-button-red-tone.md`
- 로컬: `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1743-item-list-search-alignment.md`
- 로컬: `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1750-item-list-category-select.md`
- 로컬: `doc/dev-c/.person/reports/admin-item-navigation/report-20260323-1812-item-list-searchbar-attached.md`

## 남은 TODO / 리스크
- 브라우저에서 실제 관리자 물품 목록 화면을 열어 검색바가 리스트 카드 상단에 자연스럽게 붙어 보이는지 확인이 필요하다.
- `조회`, `초기화`, 카테고리 select, 실시간 검색이 함께 있을 때 사용자 체감이 과하지 않은지 UI 확인이 필요하다.
- 현재 worklog는 `task-번호` 리포트 폴더가 아니라 `admin-item-navigation` 기능 폴더 기준으로 통합한 문서다.
