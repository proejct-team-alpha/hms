# W3-8번째작업 Report — 관리자 물품 출고 기능

## 작업 개요

- **날짜:** 2026-03-17
- **담당:** dev-a-c

---

## 구현 내용

### 1. sidebar-admin.mustache — "물품 출고" 메뉴 추가

- "물품 등록"과 "입출고 내역" 사이에 추가
- 아이콘: `package`, 활성 플래그: `isAdminItemUse`

### 2. AdminItemController — 출고 엔드포인트 추가

- `ItemManagerService` 필드 주입 추가
- `GET /admin/item/use`: `items`, `todayLogs` 모델 → `admin/item-use` 반환
  - `isAdminItemUse = true` 모델에 추가
- `POST /admin/item/use` (`@ResponseBody`): amount 검증 → `useItem(id, amount, null)` 호출
  - 성공: `{"quantity": N}` / 재고 부족·오류: 400 + `{"error": "..."}`

### 3. admin/item-use.mustache — 신규 생성

- **오늘 출고 내역** (상단): 테이블 또는 "없습니다." 표시
- **물품 선택** (하단): 카테고리 필터 + 초성·영문 검색 + 4열 카드 그리드
- 버튼명 "출고"
- AJAX: 출고 성공 시 재고 즉시 갱신, 재고 부족 시 `alert()`
- `{{> common/sidebar-admin}}`, `{{> common/header-staff}}`, `{{> common/footer-staff}}` 사용

### 4. AdminReservationController — @RequestParam name 명시 (버그 수정)

- `-parameters` 컴파일러 플래그 없는 환경에서 `int` 파라미터 이름 추론 실패 오류 수정
- `@RequestParam(defaultValue = "1") int page` → `@RequestParam(name = "page", defaultValue = "1") int page` (size, status 동일)

---

## 변경 파일

| 파일 | 변경 내용 |
|------|----------|
| `templates/common/sidebar-admin.mustache` | "물품 출고" 메뉴 추가 |
| `admin/item/AdminItemController.java` | `ItemManagerService` 주입, `GET/POST /admin/item/use` 추가 |
| `templates/admin/item-use.mustache` | 신규 생성 |
| `admin/reservation/AdminReservationController.java` | `@RequestParam` name 명시 (버그 수정) |

---

## 결과

- 관리자 사이드바 "물품 출고" 메뉴 정상 표시 및 활성 상태 강조
- 물품 출고 페이지: 카테고리 필터·초성 검색·AJAX 출고 정상 작동
- 출고 시 `ItemUsageLog` 저장 (reservationId=null, staff와 동일)
- 오늘 출고 내역 실시간 반영 (페이지 재방문 시)

---

> **💡 입문자 설명**
>
> **관리자에게도 물품 출고 기능이 필요한 이유**
> - 원무과(staff)와 동일한 비품 출고 기능이지만, 관리자는 별도의 URL(`/admin/item/use`)과 사이드바를 씁니다. Spring Security에서 역할(ROLE)별로 접근 권한을 분리하기 때문에, 같은 기능이라도 역할별 컨트롤러와 URL이 따로 필요합니다.
>
> **`AdminReservationController` `@RequestParam name` 명시 — 왜 필요했는지**
> - Java는 기본적으로 컴파일 시 파라미터 이름 정보를 바이트코드에 포함하지 않습니다. `-parameters` 컴파일 플래그가 없는 환경에서는 Spring이 `int page` 같은 원시 타입 파라미터 이름을 추론하지 못합니다.
> - `@RequestParam(name = "page", defaultValue = "1") int page`처럼 이름을 명시하면 어떤 환경에서도 안정적으로 동작합니다. 이런 버그는 개발 환경과 빌드 환경의 컴파일 설정 차이에서 발생합니다.
>
> **`ItemManagerService` 주입 — 기존 서비스를 재사용하는 이유**
> - 물품 출고 로직(`useItem()`)은 이미 `ItemManagerService`에 구현되어 있습니다. 관리자 컨트롤러에서 같은 서비스를 주입해 쓰면 코드 중복 없이 기능을 재사용합니다. 동일한 비즈니스 로직을 여러 컨트롤러가 공유하는 것이 서비스 레이어의 핵심 역할입니다.
>
> **쉽게 말하면**: 원무과 창구에 있던 출고 기능을 관리자 창구에도 복사한 작업입니다. 출고 처리 로직은 동일하고, 창구(URL·권한)만 다릅니다. 추가로, 예약 목록 페이지의 페이징 버그(페이지 번호가 전달 안 되던 문제)도 함께 수정했습니다.
