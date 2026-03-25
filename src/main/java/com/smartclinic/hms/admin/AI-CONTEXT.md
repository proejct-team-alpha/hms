<!-- Parent: ../AI-CONTEXT.md -->

# admin — 관리자

## 목적

ADMIN 역할의 전체 시스템 관리. 대시보드 통계, 직원/진료과/물품/규칙 CRUD, 예약·환자·접수 조회.
**개발자 C(강상민) 소유.**

## 하위 패키지

| 패키지 | 설명 |
|--------|------|
| `dashboard/` | 대시보드 통계 (오늘예약/총예약/직원수/재고부족) |
| `department/` | 진료과 CRUD |
| `item/` | 물품 CRUD |
| `patient/` | 환자 조회 |
| `reception/` | 접수 목록 조회 |
| `reservation/` | 예약 목록·취소 |
| `rule/` | 병원 규칙 CRUD |
| `staff/` | 직원 CRUD |

## 주요 파일

| 파일 | 설명 |
|------|------|
| AdminDashboardController.java | GET /admin/dashboard |
| AdminDashboardApiController.java | REST API (통계 차트 등) |
| AdminDashboardStatsService.java | 대시보드 통계 4종 + 차트 조회 |
| dto/AdminDashboardStatsResponse.java | 통계 응답 DTO |
| dto/AdminDashboardChartResponse.java | 차트 응답 DTO (TODO) |
| AdminDepartmentController.java | CRUD /admin/department/** |
| AdminItemController.java | CRUD /admin/item/** |
| AdminStaffController.java | CRUD /admin/staff/** |
| AdminRuleController.java | CRUD /admin/rule/** |
| AdminPatientController.java | 환자 목록/상세 |
| AdminReceptionController.java | 접수 목록 |
| AdminReservationController.java | 예약 목록/취소 |
| AdminReservationRepository.java | 예약 통계/조회 |
| DepartmentRepository.java | Department CRUD |
| ItemRepository.java | Item CRUD |
| AdminStaffRepository.java | Staff CRUD |
| HospitalRuleRepository.java | HospitalRule CRUD |

## AI 작업 지침

- ADMIN 접근: `/admin/**` → `hasRole("ADMIN")`
- Repository 네이밍 금지: `AdminDashboardStatsRepository`, `DashboardRepository`, `StatsRepository`
- 차트 API: `GET /admin/api/dashboard/chart` → JSON 응답 (`Resp.ok()`)
- TASK-devC-005: `AdminDashboardStatsService.getDashboardChart()` 미완성 (TODO)

## 테스트

```bash
# 관련 테스트 파일
src/test/java/com/smartclinic/hms/admin/dashboard/
├── AdminDashboardControllerTest.java
├── AdminDashboardApiControllerTest.java
├── AdminDashboardStatsServiceTest.java
└── AdminDashboardStatsRepositoryTest.java
```

## 의존성

- 내부: `domain/` 전반, `common/`
- 뷰: `templates/admin/`
