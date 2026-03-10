<!-- Parent: ../AI-CONTEXT.md -->

# staff — 스태프

## 목적

STAFF 역할 직원의 업무 처리. 예약 접수(RESERVED→RECEIVED), 방문 접수, 전화 예약.
**개발자 B(조유지) 소유.**

## 하위 패키지

| 패키지 | 설명 |
|--------|------|
| `dashboard/` | StaffDashboardController — GET /staff/dashboard |
| `reception/` | 예약 접수 처리 (RESERVED → RECEIVED 상태 전이) |
| `reception/dto/` | ReceptionUpdateRequest DTO |
| `walkin/` | WalkinController + WalkinService — 방문 즉시 접수 |
| `reservation/` | PhoneReservationController — 전화 예약 (GET new / POST create) |

## 주요 파일

| 파일 | 설명 |
|------|------|
| StaffDashboardController.java | GET /staff/dashboard |
| ReceptionController.java | GET/POST /staff/reception/** |
| ReceptionService.java | RESERVED→RECEIVED 상태 전이 |
| ReceptionUpdateRequest.java | 접수 처리 요청 DTO |
| WalkinController.java | GET/POST /staff/walkin/** |
| WalkinService.java | 방문 접수 `@Transactional` |
| PhoneReservationController.java | GET /staff/reservation/new, POST /staff/reservation/create |

## AI 작업 지침

- STAFF 접근 권한: `SecurityConfig`에서 `/staff/**` → `hasRole("STAFF")` 이상
- 상태 전이 로직은 Service에서만 처리
- `@Transactional` 쓰기 메서드에만 명시적 적용

## 의존성

- 내부: `domain/Reservation`, `domain/ReservationStatus`, `common/service/`
- 뷰: `templates/staff/`
