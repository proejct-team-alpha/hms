# 개발자 C (강상민) — SKILL.md

> **역할:** 관리자 대시보드, 예약/환자/직원/진료과/물품/규칙 관리, REST API
> **담당 URL:** `/admin/**`, `/api/**`
> **담당 화면:** S17~S42 (관리자 16개 화면 + 카테고리 관리)
> **기준 문서:** PRD v1.0 / 프로젝트 계획서 v4.3 / 화면 기능 정의서 v5.1

---

## 1. 소유 영역

### 1.1 코드 소유권

| 영역 | 경로 | 설명 |
|------|------|------|
| **admin/Controller** | `admin/dashboard/AdminDashboardController.java` | 대시보드 + 통계 AJAX |
| **admin/Controller** | `admin/reservation/AdminReservationController.java` | 예약 목록, 접수 현황, 취소 |
| **admin/Controller** | `admin/patient/AdminPatientController.java` | 환자 목록, 상세 |
| **admin/Controller** | `admin/staff/AdminStaffController.java` | 직원 CRUD |
| **admin/Controller** | `admin/department/AdminDepartmentController.java` | 진료과 CRUD |
| **admin/Controller** | `admin/item/AdminItemController.java` | 물품 CRUD |
| **admin/Controller** | `admin/item/AdminItemCategoryController.java` | 물품 카테고리 CRUD |
| **admin/Controller** | `admin/rule/AdminRuleController.java` | 규칙 CRUD |
| **admin/Controller** | `admin/rule/AdminRuleCategoryController.java` | 규칙 카테고리 CRUD |
| **admin/Service** | `admin/**Service.java` | 각 관리자 비즈니스 로직 |
| **api/Controller** | `api/ApiController.java` | JSON API 레이어 (/api/**) |
| **Repository** | `admin/**Repository.java` | 관리자 데이터 접근 |
| **DTO** | `admin/**/dto/**` | 각 관리자 DTO |
| **Template** | `templates/admin/**` | 관리자 전체 화면 (16개+) |

### 1.2 담당 레이아웃

- **L2** (직원): header-staff → sidebar-admin + main → footer-staff
- **S4** 사이드바 (ADMIN): 대시보드, 예약관리, 환자관리, 인사관리, 진료과관리, 물품관리, 규칙관리

### 1.3 Git 브랜치

```
feature/admin-dashboard
feature/admin-reservation
feature/admin-staff
feature/admin-department
feature/admin-item
feature/admin-rule
feature/api-layer
```

---

## 2. 관련 PRD 기능

| PRD 기능 | 세부 항목 | 역할 |
|----------|----------|------|
| **F07** 관리자 — 예약/접수 관리 | F07-1 ~ F07-3 전체 | 전담 구현 |
| **F08** 관리자 — 환자 관리 | F08-1 ~ F08-2 전체 | 전담 구현 |
| **F09** 관리자 — 인사 관리 | F09-1 ~ F09-4 전체 | 전담 구현 |
| **F10** 관리자 — 진료과 관리 | F10-1 ~ F10-5 전체 | 전담 구현 |
| **F11** 관리자 — 물품 관리 | F11-1 ~ F11-4 전체 | 전담 구현 |
| **F12** 관리자 — 규칙 관리 | F12-1 ~ F12-6 전체 | 전담 구현 |
| **F13** 관리자 — 대시보드 | F13-1 ~ F13-2 전체 | 전담 구현 |
| **F14** JSON API 레이어 | F14-1 ~ F14-5 전체 | 전담 구현 |

---

## 3. 주차별 작업 상세

### W1 — 관리자 대시보드 기반 구축

**목표:** 관리자 대시보드 기본 구조 완성, ADMIN 레이아웃 적용 확인
**의존:** 책임개발자(김민구)의 Entity·LayoutModelInterceptor 완성 (W1 후반)

#### W1 전반 (Day 1~2)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 대시보드(S17) 구조 설계 | 4개 통계 영역 구성 | 레이아웃 초안 |
| 2 | `AdminDashboardController` 구현 | `GET /admin/dashboard` | 대시보드 렌더링 |

#### W1 후반 (Day 3~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 3 | 4개 통계 구현 | 전체 환자, 오늘 예약, 직원 수, 진료과 수 | DB 조회 후 표시 |
| 4 | L2 + S4 사이드바 적용 확인 | 관리자 메뉴 렌더링 | 계층형 메뉴 정상 표시 |
| 5 | CRUD 공통 템플릿 패턴 확립 | 목록/상세/등록/수정 Mustache 구조 | 재사용 가능한 패턴 |

#### W1 체크포인트

- [ ] ADMIN 로그인 → 대시보드 이동
- [ ] 4개 통계 표시
- [ ] S4 사이드바 메뉴 완성

---

### W2 — 예약 관리 & 직원/진료과 CRUD

**목표:** 예약 관리 (목록+취소), 직원 CRUD, 진료과 CRUD 전체 완성
**의존:** 없음 (독립 작업)

#### W2 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 예약 목록(S19) | 페이징 + 상태 필터링 | 날짜/상태/진료과 필터 |
| 2 | 예약 취소 | POST `/admin/reservation/cancel` | RESERVED→CANCELLED (COMPLETED는 취소 불가) |
| 3 | 직원 목록(S22) | 검색 + 역할 필터 | 페이징 동작 |
| 4 | 직원 등록(S23) | POST `/admin/staff/create` | BCrypt 비밀번호 암호화, 의사 시 DOCTOR 자동 생성 |
| 5 | 직원 상세/수정(S24) | GET/POST | 직원 정보 수정 |

#### W2 후반 (Day 4~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 6 | REST API — `POST /api/staff/{id}/update` | JSON 응답 | 의사 전문분야/진료요일 수정 |
| 7 | 진료과 목록(S25) | 페이징 | 활성/비활성 목록 |
| 8 | 진료과 상세(S26) | 소속 의사 + 통계 | 상세 정보 표시 |
| 9 | 진료과 등록/수정/활성화/비활성화 | CRUD 전체 | 비활성화 시 예약에서 제외 |
| 10 | @Valid 유효성 검증 | 직원/진료과 DTO | 필수 필드 검증 |
| 11 | 단위 테스트 | Service/Controller 테스트 | 전체 통과 |
| 12 | PR 제출 & 리뷰 반영 | → `develop` PR | 책임개발자 리뷰 승인 |

#### W2 테스트

| 테스트 | 유형 | 검증 내용 |
|--------|------|-----------|
| `AdminStaffServiceTest` | 단위 (Mockito) | 직원 CRUD, BCrypt, 중복 방지 |
| `AdminDepartmentServiceTest` | 단위 (Mockito) | 진료과 CRUD, 활성화/비활성화 |
| `AdminReservationControllerTest` | MockMvc | 목록 GET, 취소 POST, ROLE_ADMIN 전용 |

#### W2 체크포인트

- [ ] 예약 목록 페이징 + 필터링 동작
- [ ] 예약 취소 동작 (COMPLETED 취소 차단)
- [ ] 직원 CRUD 전체 동작
- [ ] 진료과 CRUD 전체 동작
- [ ] REST API 동작 (`POST /api/staff/{id}/update`)
- [ ] @Valid 유효성 검증
- [ ] 단위 테스트 통과
- [ ] PR 리뷰 승인 & develop 머지

---

### W3 — 접수 현황 & 물품/규칙 CRUD

**목표:** 접수 현황, 환자 관리, 물품 CRUD 완성, 규칙 CRUD 골격
**의존:** 없음 (독립 작업)

#### W3 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 접수 현황(S18) | 당일 접수 통합 뷰 | 소스 라벨 (ONLINE/PHONE/WALKIN) 표시 |
| 2 | 환자 목록(S20) | 검색 + 페이징 | 환자 디렉토리 |
| 3 | 환자 상세(S21) | 프로필 + 예약 이력 | 전체 이력 표시 |
| 4 | REST API — `POST /api/patients/{id}/update` | JSON 응답 | 환자 정보 수정 |
| 5 | 물품 목록(S27) | 카테고리 필터링 | 재고 상태별 필터 |
| 6 | 물품 등록(S28) | POST `/admin/item/create` | 이름, 카테고리, 수량, 최소수량 |

#### W3 후반 (Day 4~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 7 | 물품 상세/수정(S29) | GET/POST | 물품 정보 수정 |
| 8 | REST API — `POST /api/items/{id}/delete` | JSON 응답 | 물품 삭제 |
| 9 | 규칙 CRUD 골격(S30) | 규칙 목록 기본 | 카테고리별 필터링 |
| 10 | 단위 테스트 | 물품/규칙 Service 테스트 | 전체 통과 |
| 11 | PR 제출 | `feature/admin-item` → `develop` | 리뷰 승인 |

#### W3 테스트

| 테스트 | 유형 | 검증 내용 |
|--------|------|-----------|
| `AdminItemServiceTest` | 단위 (Mockito) | 물품 CRUD, 카테고리 필터링 |
| `AdminRuleServiceTest` | 단위 (Mockito) | 활성 규칙 조회, 페이징 |

#### W3 체크포인트

- [ ] 접수 현황 페이징 + 소스 라벨 표시
- [ ] 환자 목록/상세 동작
- [ ] 물품 CRUD 전체 완성
- [ ] REST API 동작 (`POST /api/patients/{id}/update`, `POST /api/items/{id}/delete`)
- [ ] 규칙 목록 기본 렌더링
- [ ] 단위 테스트 통과
- [ ] PR 리뷰 승인 & develop 머지

---

### W4 — 규칙 CRUD 완성 & 최종 마무리

**목표:** 병원 규칙 CRUD 완성, 카테고리 관리, 전체 16개 화면 최종 점검
**의존:** 없음 (독립 작업)

#### W4 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 규칙 목록(S30) 완성 | 페이징 + 카테고리 필터 | 검색 동작 |
| 2 | 규칙 등록(S31) | POST `/admin/rule/create` | 제목, 카테고리, 내용(3000자), 활성 상태 |
| 3 | 규칙 상세/수정(S32) | GET/POST | 규칙 내용 수정 |
| 4 | 규칙 활성화 토글 | POST `/admin/rule/toggleActive` | 즉시 챗봇 컨텍스트 반영 |
| 5 | REST API — `POST /api/rules/{id}/delete` | JSON 응답 | 규칙 삭제 |
| 6 | 물품 카테고리 CRUD (S37~39) | 목록/등록/수정/활성화/비활성화 | 동적 카테고리 관리 |
| 7 | 규칙 카테고리 CRUD (S40~42) | 목록/등록/수정/활성화/비활성화 | 동적 카테고리 관리 |
| 8 | REST API — `POST /api/reservations/{id}/cancel` | JSON 응답 | 예약 취소 |

#### W4 후반 (Day 4~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 9 | 대시보드 통계 AJAX | `GET /admin/dashboard/stats` | 실시간 통계 갱신 (5개 항목) |
| 10 | 전체 16개 화면 감사 | 모든 관리자 화면 점검 | 레이아웃·기능 정상 |
| 11 | 대시보드 통계 정확성 검증 | DB 데이터 대비 통계 일치 | 수치 정확 |
| 12 | 버그 수정 | 통합 테스트 발견 이슈 | 책임개발자 리포트 기준 |
| 13 | PR 제출 & 리뷰 반영 | `feature/admin-rule` → `develop` | 리뷰 승인 |
| 14 | 배포 지원 | 운영 서버 확인 | 관리자 기능 동작 확인 |

#### W4 테스트

| 테스트 | 유형 | 검증 내용 |
|--------|------|-----------|
| `AdminRuleServiceTest` | 단위 | 규칙 CRUD, 활성화 토글 |
| `AdminDashboardControllerTest` | MockMvc | 통계 JSON, ROLE_ADMIN 전용 |
| `ApiControllerTest` | MockMvc | 5개 REST API 동작 |

#### W4 체크포인트

- [ ] 규칙 CRUD 전체 완성
- [ ] 규칙 활성화 토글 동작 (챗봇 즉시 반영)
- [ ] 물품/규칙 카테고리 CRUD 완성
- [ ] 5개 REST API 전체 동작
- [ ] 대시보드 통계 AJAX 갱신 동작
- [ ] 16개 관리자 화면 감사 완료
- [ ] PR 리뷰 승인 & develop 머지
- [ ] 운영 서버 동작 확인

---

## 4. REST API 담당 (PBL 필수)

| # | 메서드 | 경로 | 설명 | 주차 |
|---|--------|------|------|------|
| 1 | POST | `/api/staff/{id}/update` | 의사 전문분야/진료요일 수정 | W2 |
| 2 | POST | `/api/patients/{id}/update` | 환자 정보 수정 | W3 |
| 3 | POST | `/api/reservations/{id}/cancel` | 예약 취소 | W4 |
| 4 | POST | `/api/items/{id}/delete` | 물품 삭제 | W3 |
| 5 | POST | `/api/rules/{id}/delete` | 규칙 삭제 | W4 |

**응답 형식:** `{ success: boolean, data/errorCode: any, message: string }`

---

## 5. 대시보드 통계 항목

| # | 통계 | 데이터 소스 | 갱신 방식 |
|---|------|------------|-----------|
| 1 | 오늘 예약 건수 | `Reservation` (today) | SSR + AJAX |
| 2 | 전체 예약 건수 | `Reservation` (all) | SSR + AJAX |
| 3 | 진료과별 예약 분포 | `Reservation` GROUP BY department | SSR + AJAX |
| 4 | 재고 부족 물품 수 | `Item` (quantity < minQuantity) | SSR + AJAX |
| 5 | 직원 수 | `Staff` (active) | SSR + AJAX |

---

## 6. 절대 터치 금지 영역

| 파일/디렉터리 | 소유자 | 접근 수준 |
|---------------|--------|-----------|
| `config/SecurityConfig.java` | 책임개발자(김민구) | 읽기 전용 |
| `domain/*.java` (Entity) | 책임개발자(김민구) | 접근 금지 (수정 시 Issue) |
| `common/service/SlotService.java` | 책임개발자(김민구) | 인터페이스 호출만 |
| `llm/LlmService.java` | 책임개발자(김민구) | 인터페이스 호출만 |
| `reservation/**` | 개발자 A(강태오) | 접근 금지 |
| `staff/**`, `doctor/**`, `nurse/**` | 개발자 B(조유지) | 접근 금지 |
| `ReservationRepository` | 개발자 A(강태오) | 조회만 허용 (save 금지) |
