# 개발자 B (조유지) — SKILL.md

> **역할:** STAFF·DOCTOR·NURSE 대시보드, 접수 처리, 방문 접수, 진료 기록, 챗봇 UI
> **담당 URL:** `/staff/**`, `/doctor/**`, `/nurse/**`
> **담당 화면:** S05~S16 (STAFF 6개 + DOCTOR 4개 + NURSE 4개) + C01 (챗봇)
> **기준 문서:** PRD v1.0 / 프로젝트 계획서 v4.3 / 화면 기능 정의서 v5.1

---

## 1. 소유 영역

### 1.1 코드 소유권

| 영역 | 경로 | 설명 |
|------|------|------|
| **staff/Controller** | `staff/ReceptionController.java` | 접수 목록, 접수 처리 |
| **staff/Controller** | `staff/PhoneReservationController.java` | 전화 예약 |
| **staff/Controller** | `staff/WalkinController.java` | 방문 접수 |
| **staff/Controller** | `staff/StaffMypageController.java` | STAFF 내 정보 관리 |
| **staff/Service** | `staff/ReceptionService.java` | 접수 처리 비즈니스 로직 |
| **staff/Service** | `staff/WalkinService.java` | 방문 접수 로직 |
| **doctor/Controller** | `doctor/TreatmentController.java` | 진료 목록, 상세, 완료 |
| **doctor/Service** | `doctor/TreatmentService.java` | 진료 완료 + TreatmentRecord 저장 |
| **doctor/Controller** | `doctor/DoctorMypageController.java` | DOCTOR 내 정보 관리 |
| **nurse/Controller** | `nurse/ScheduleController.java` | 스케줄 목록 |
| **nurse/Controller** | `nurse/PatientUpdateController.java` | 환자 정보 수정 |
| **nurse/Controller** | `nurse/NurseMypageController.java` | NURSE 내 정보 관리 |
| **Template** | `templates/staff/**` | STAFF 전체 화면 |
| **Template** | `templates/doctor/**` | DOCTOR 전체 화면 |
| **Template** | `templates/nurse/**` | NURSE 전체 화면 |
| **Template** | `templates/common/chatbot-overlay.mustache` | 챗봇 오버레이 UI |

### 1.2 담당 레이아웃

- **L2** (직원): header-staff → sidebar-{role} + main → footer-staff
- **S1** 사이드바 (STAFF): 접수 대시보드, 예약 목록, 전화 예약, 방문 접수, 내 정보
- **S2** 사이드바 (DOCTOR): 대시보드, 진료 목록, 챗봇, 내 정보
- **S3** 사이드바 (NURSE): 대시보드, 스케줄, 챗봇, 내 정보

### 1.3 Git 브랜치

```
feature/reception
feature/walkin
feature/phone-reservation
feature/staff-dashboard
feature/doctor-treatment
feature/nurse
feature/llm-chatbot-ui
```

---

## 2. 관련 PRD 기능

| PRD 기능 | 세부 항목 | 역할 |
|----------|----------|------|
| **F03** 접수 직원 업무 | F03-1 ~ F03-5 전체 | 전담 구현 |
| **F04** 의사 업무 | F04-1 ~ F04-4 전체 | 전담 구현 |
| **F05** 간호사 업무 | F05-1 ~ F05-4 전체 | 전담 구현 |
| **F06** LLM 통합 | F06-2 챗봇, F06-3 대화 이력 | UI 연동 (LlmService는 책임개발자) |

---

## 3. 주차별 작업 상세

### W1 — STAFF 로그인 & 대시보드 골격

**목표:** STAFF 로그인 동선 확인, 대시보드 기본 골격 구현
**의존:** 책임개발자(김민구)의 SecurityConfig·LayoutModelInterceptor 완성 (W1 후반)

#### W1 전반 (Day 1~2)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | STAFF 로그인 화면(S05) 구조 확인 | 로그인 폼 | Spring Security 연동 |
| 2 | 로그인 동선 확인 | 로그인 → 대시보드 | ROLE별 대시보드 리다이렉트 |

#### W1 후반 (Day 3~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 3 | `StaffDashboardController` 구현 | `GET /staff/dashboard` | STAFF 대시보드 렌더링 |
| 4 | STAFF 대시보드(S06) Mustache 작성 | `staff/dashboard.mustache` | 오늘 접수 대기 건수 표시 |
| 5 | L2 + S1 사이드바 적용 확인 | 사이드바 렌더링 | STAFF 메뉴 정상 표시 |

#### W1 체크포인트

- [ ] STAFF 로그인 → 대시보드 이동
- [ ] 대시보드 기본 골격 렌더링
- [ ] S1 사이드바 메뉴 표시

---

### W2 — 접수 처리 & 방문 접수 전체 완성

**목표:** 접수 목록 → 접수 처리, 방문 접수, 전화 예약 전체 완성
**의존:** 책임개발자(김민구)의 SlotService (W1에 develop 머지) — 방문 접수에서 사용

#### W2 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 접수 대기 목록(S07) | `ReceptionController` GET | 오늘 RESERVED 예약 목록 + 페이징 |
| 2 | 접수 처리(S08) | POST `/staff/reception/receive` | RESERVED → RECEIVED 상태 전이 |
| 3 | `ReceptionService` 구현 | 상태 검증 + 전이 로직 | 이미 RECEIVED인 건 재접수 차단 |
| 4 | 접수 상세 화면 | 환자 추가 정보 입력 (주소·특이사항) | 접수 시 환자 정보 업데이트 |

#### W2 후반 (Day 4~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 5 | 전화 예약(S09) | `PhoneReservationController` | STAFF가 예약 등록 (RESERVED) |
| 6 | 방문 접수(S10) | `WalkinController`, `WalkinService` | Patient + Reservation(RECEIVED) 단일 트랜잭션 |
| 7 | @Valid 유효성 검증 | 접수·방문접수 DTO | 빈 필드 에러 메시지 |
| 8 | 단위 테스트 작성 | `ReceptionServiceTest` | 전이 성공, 역방향 차단 |
| 9 | PR 제출 & 리뷰 반영 | `feature/reception`, `feature/walkin` → `develop` | 책임개발자 리뷰 승인 |

#### W2 테스트

| 테스트 | 유형 | 검증 내용 |
|--------|------|-----------|
| `ReceptionServiceTest` | 단위 (Mockito) | RESERVED→RECEIVED 성공, 역방향 차단 |
| `WalkinServiceTest` | 단위 (Mockito) | Patient 생성 + RECEIVED 직행, 원자성 |
| `ReceptionControllerTest` | MockMvc | 접수 목록 GET, 접수 POST (PRG), ROLE_STAFF 전용 |

#### W2 체크포인트

- [ ] 접수 대기 목록 페이징 동작
- [ ] 접수 처리 (RESERVED → RECEIVED) 성공
- [ ] 전화 예약 등록 동작
- [ ] 방문 접수 Patient + Reservation 동시 생성
- [ ] `SlotService.validateAndLock()` 호출 확인
- [ ] 단위 테스트 전체 통과
- [ ] PR 리뷰 승인 & develop 머지

---

### W3 — 진료 기록 & DOCTOR·NURSE 화면 전체 완성

**목표:** 진료 기록 입력·완료 처리, DOCTOR·NURSE 대시보드 및 현황 화면 완성
**의존:** 없음 (독립 작업)

#### W3 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | DOCTOR 대시보드(S11) | `DoctorDashboardController` | 오늘 진료 예정/완료 건수 |
| 2 | 진료 현황(S12) | 오늘 RECEIVED 환자 목록 | 본인 담당 환자만 표시 |
| 3 | 진료 기록(S13) 입력 화면 | `TreatmentController` GET/POST | 진단·처방 입력 폼 |
| 4 | `TreatmentService` 구현 | RECEIVED→COMPLETED + TreatmentRecord 저장 | 진료 완료 처리 |
| 5 | 권한 검증 | 본인 담당 환자만 | `reservation.doctor.staffId == 로그인 staffId` |

#### W3 후반 (Day 4~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 6 | NURSE 대시보드(S14) | `NurseDashboardController` | 오늘 예약 현황 |
| 7 | 예약 현황(S15) | `ScheduleController` | 전체 상태 예약 목록 |
| 8 | 환자 정보 수정(S16) | `PatientUpdateController` | 간호사 환자 정보 수정 |
| 9 | 단위 테스트 작성 | `TreatmentServiceTest` | 전이, 권한 검증 |
| 10 | **W3 금요일 LLM DTO 미팅 참석** | ChatbotRequest/Response DTO 확인 | W4 챗봇 UI 계획 확정 |

#### W3 테스트

| 테스트 | 유형 | 검증 내용 |
|--------|------|-----------|
| `TreatmentServiceTest` | 단위 (Mockito) | RECEIVED→COMPLETED 성공, 역방향 차단, 권한 |
| `TreatmentControllerTest` | MockMvc | 진료 목록 GET, 진료 기록 POST, ROLE_DOCTOR 전용 |

#### W3 체크포인트

- [ ] DOCTOR 대시보드 + 진료 현황 + 진료 기록 전체 동작
- [ ] RECEIVED → COMPLETED 상태 전이 정상
- [ ] 본인 담당 환자만 진료 가능 (권한 검증)
- [ ] NURSE 대시보드 + 스케줄 + 환자 수정 전체 동작
- [ ] ChatbotRequest/Response DTO 인터페이스 확인 완료
- [ ] PR 리뷰 승인 & develop 머지

---

### W4 — 챗봇 UI 완성 & 최종 마무리

**목표:** 병원 규칙 Q&A 챗봇 UI 완성, 대화 이력 저장 연결
**의존:** 책임개발자(김민구)의 LlmService develop 머지 (W4 Day 1) + ChatbotRequest/Response DTO

#### W4 전반 (Day 1~3)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 1 | 챗봇 오버레이 UI | `common/chatbot-overlay.mustache` | 우측 하단 버튼 + 대화 창 토글 |
| 2 | 챗봇 질의 AJAX 연결 | `fetchWithCsrf('POST /llm/rules/ask')` | 질문 → 답변 → 렌더링 |
| 3 | 대화 이력 표시 | 대화 목록 동적 추가 | 질문·답변 순차, 스크롤 자동 |
| 4 | 대화 이력 저장 연결 | ChatbotHistory DB 저장 | 세션 단위 이력 저장 |

#### W4 후반 (Day 4~5)

| # | 작업 | 산출물 | 완료 기준 |
|---|------|--------|-----------|
| 5 | 챗봇 접근 권한 확인 | DOCTOR/NURSE만 표시 | STAFF/ADMIN에서 버튼 미표시 |
| 6 | 챗봇 에러 처리 | API 실패 시 안내 | "잠시 후 다시 시도해 주세요" |
| 7 | 전체 UI 최종 점검 | STAFF·DOCTOR·NURSE 전체 | 모든 화면 정상 동작 |
| 8 | 버그 수정 | 통합 테스트 이슈 수정 | 책임개발자 리포트 기준 |
| 9 | PR 제출 & 리뷰 반영 | `feature/llm-chatbot-ui` → `develop` | 리뷰 승인 |
| 10 | 배포 지원 | 운영 서버 확인 | 접수·진료·챗봇 동작 확인 |

#### W4 체크포인트

- [ ] 챗봇 UI 정상 동작 (질문 → 답변 표시)
- [ ] 대화 이력 DB 저장 확인
- [ ] DOCTOR/NURSE에서만 챗봇 표시
- [ ] API 실패 시 에러 메시지 표시
- [ ] CSRF 토큰 AJAX 처리 동작
- [ ] 전체 STAFF·DOCTOR·NURSE 화면 최종 점검
- [ ] PR 리뷰 승인 & develop 머지
- [ ] 운영 서버 동작 확인

---

## 4. 담당 상태 전이

| 전이 | 트리거 | Service |
|------|--------|---------|
| RESERVED → RECEIVED | `POST /staff/reception/receive` | `ReceptionService` |
| RECEIVED → COMPLETED | `POST /doctor/treatment/complete` | `TreatmentService` |

**상태 전이 독점 원칙:** 각 전이는 해당 Service만 처리. 다른 Service에서 동일 전이 호출 금지.

---

## 5. 절대 터치 금지 영역

| 파일/디렉터리 | 소유자 | 접근 수준 |
|---------------|--------|-----------|
| `config/SecurityConfig.java` | 책임개발자(김민구) | 읽기 전용 |
| `domain/*.java` (Entity) | 책임개발자(김민구) | 접근 금지 (수정 시 Issue) |
| `common/service/SlotService.java` | 책임개발자(김민구) | 인터페이스 호출만 |
| `llm/LlmService.java` | 책임개발자(김민구) | 인터페이스 호출만 |
| `reservation/**` | 개발자 A(강태오) | 접근 금지 |
| `admin/**` | 개발자 C(강상민) | 접근 금지 |
