# 코드 리뷰 — Lead(김민구) 전용 수정 작업 문서

**원본 문서:** `doc/CODE_REVIEW_2026-03-20.md`
**담당자:** 김민구 (책임개발자 / Lead)
**담당 영역:** `config/`**, `common/**`, `domain/**`, `llm/**`, `application*.properties`, `templates/common/**`

**코드베이스 검증:** 2026-03-21, Spring Boot `4.0.3`, 루트 `src/main/java`·`src/test/java` 기준 정적 분석

---

## 0. 코드베이스 검증 요약 (전체 분석)

리뷰 문서(2026-03-20) 작성 이후 코드가 일부 반영되어 있음. 아래 **상태** 컬럼을 기준으로 우선순위를 재조정할 것.


| 구분        | 미착수 | 진행/부분 | 완료(코드상)   |
| --------- | --- | ----- | --------- |
| CRITICAL  | 0   | 1     | 2         |
| HIGH      | 2   | 1     | 2         |
| MEDIUM    | 3   | 1     | 1         |
| LOW       | 3   | 0     | 0         |
| **분석 신규** | —   | —     | 4건 (§0.2) |


### 0.1 리뷰 ID별 검증 결과


| ID        | 리뷰 제목 (요약)                       | 검증 결과                                                                                                                                                                                                                                      |
| --------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| C-01      | tools.jackson import             | **부분 완료.** SB4/Jackson3 `tools.jackson` 유지. `toJson` 실패 시 `IllegalStateException` 전파로 조용한 `"{}"` 제거(2026-03-21). `LlmResponse` 등 `com.fasterxml` 어노테이션 혼용은 **통일 검토** 잔여.                                                                   |
| C-02      | GlobalExceptionHandler SSR 공백    | **부분 완화.** `@RestController`는 `GlobalExceptionHandler`, `@Controller`(SSR)는 `SsrExceptionHandler`가 담당하는 **이중 구조**로 동작. 문서/온보딩 관점에서 **역할 주석·AI-CONTEXT 정합성** 정리 권장.                                                                         |
| C-04      | 히스토리 소유권                         | **완료.** `ChatController`·`MedicalController` 모두 `securityUtils.resolveStaffId()`와 경로 `staffId` 일치 검증 후 조회.                                                                                                                                 |
| H-01      | `Reservation.cancel()` 전이        | **완료(2026-03-21).** `IN_TREATMENT`에서 `cancel()` 시 명시적 예외(무전이 방지). 엔티티 Javadoc에 `cancel` vs `cancelFully`·호출 맵. `ReceptionService.cancel`에서 `IllegalStateException`→`CustomException.invalidStatusTransition`. `ReservationTest` 단위 테스트 5건. |
| H-02      | Entity별 다중 Repository            | **미착수.** `Reservation` 4·`Patient` 3·`Staff` 2·`Department` 2 등 구조 유지(Repository 25개 + `_sample` 1개).                                                                                                                                      |
| H-03      | `resolveStaffId()` 중복            | **완료.** `common/util/SecurityUtils` 단일 빈으로 통합, LLM 컨트롤러에서 주입 사용.                                                                                                                                                                           |
| H-06      | `LlmReservationService` N+1      | **완료.** `reservationRepository.findBookedSlotsBetween` 일괄 조회 + `Set` 매칭으로 루프 내 카운트 제거.                                                                                                                                                     |
| H-07      | LLM Entity `@Setter`             | **완료(확장).** `domain` 패키지 전체 `@Setter` **0건** (grep 기준).                                                                                                                                                                                    |
| M-01      | Resp vs ErrorResponse            | **부분.** 별도 `ErrorResponse` 타입은 없음. `GlobalExceptionHandler`는 `Resp.fail(..., errorCode, msg)` 사용. 주석의 JSON 필드 설명과 `Resp`(status/msg/body) 필드 **불일치** — 스키마·클라이언트 계약 정리 권장.                                                                 |
| M-02      | SsrExceptionHandler 상태코드         | **완료.** `CustomException.getHttpStatus()`로 403/404/500 뷰 분기, `res.setStatus` 반영, `errorMessage` 모델 전달.                                                                                                                                     |
| M-06      | TestSecurityConfig 중복            | **부분 완료(2026-03-21).** `LlmWebMvcTestSecurityConfig`로 LLM WebMvcTest **5건** 통합 (`Chat`/`Medical`/`Symptom`/`LlmReservation`/`LlmPage`). 잔여 내부 설정: Reservation·Item·Doctor·Admin API 등 **약 7건**.                                            |
| M-07      | LLM 서비스 단위 테스트                   | **완료(기본).** `LlmReservationServiceTest`, `SymptomAnalysisServiceTest` 존재·Given-When-Then·BDDMockito 사용. 엣지 케이스·WebClient 오류 경로 보강은 선택.                                                                                                     |
| M-09      | `_sample` 운영 빌드 포함               | **미착수.** `com.smartclinic.hms._sample` 패키지가 main 소스에 그대로 존재.                                                                                                                                                                               |
| L-01~L-03 | Doctor 정규화·Interceptor·StaffRole | **미착수.** 리뷰 내용과 동일 가정.                                                                                                                                                                                                                     |


### 0.2 전체 분석에서 추가한 작업 (신규 TASK)


| 신규 ID        | 작업                                                                                                                 | 근거                                              | 우선순위   |
| ------------ | ------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------- | ------ |
| **T-NEW-01** | `ChatController`·`MedicalController`에서 **미사용** `StaffRepository` 필드·생성자 의존성 제거                                     | `resolveStaffId()`는 `SecurityUtils`만 사용, 필드 미참조 | LOW    |
| **T-NEW-02** | `MedicalService.toJson()` — 직렬화 실패 시 `"{}"` 반환 대신 **예외 전파** 또는 최소 **명시적 실패 표시**(빈 객체 숨김 방지)                        | 메타데이터 손실·디버깅 난이도                                | MEDIUM |
| **T-NEW-03** | LLM 히스토리 조회: `ChatbotHistoryRepository` / `MedicalHistoryRepository`를 컨트롤러에서 직접 호출하지 않고 **전용 Application 서비스**로 이동 | `rule-controller` "Repository 직접 호출 금지"         | MEDIUM |
| **T-NEW-04** | `GlobalExceptionHandler` 주석·`common/exception/AI-CONTEXT.md`와 실제 `Resp` 스키마 **용어·필드 정합성** 정리                       | M-01과 연계, 신규 기여자 혼란 방지                          | LOW    |


### 0.3 Repository·테스트 인벤토리 (참고)

**main `*Repository.java` (25 + sample 1):**
`reservation`: Reservation, Department, Patient — `nurse`: NurseReservation, NursePatient — `doctor`: DoctorReservation, DoctorTreatmentRecord, Doctor — `admin`: AdminReservation, AdminPatient, AdminStaff, AdminDepartment, Item, HospitalRule — `auth`: Staff — `item`: ItemManager, ItemUsageLog, ItemStockLog — `domain`: MedicalHistory, MedicalQa, MedicalDomain, MedicalContent, DoctorSchedule, ChatbotHistory — `_sample`: SampleReservationRepository

**공통 테스트 보안:** `common/LlmWebMvcTestSecurityConfig` — 위 5개 LLM 컨트롤러 WebMvcTest에서 `@Import`  
**내부 `TestSecurityConfig` 잔여 (예시):** `ReservationControllerTest`, `ItemManagerControllerTest`, `DoctorTreatmentControllerTest`, `DoctorDashboardControllerTest`, `AdminStaffApiControllerTest`, `AdminReservationApiControllerTest`, `AdminDepartmentControllerTest`, `AdminDashboardApiControllerTest` … (+ `AdminControllerTestSecurityConfig` 사용 Admin SSR/API 일부)

---

## 1. 규칙 준수 점검 — Lead 배정 항목 (리뷰 원문)

### 1.1 rule-controller.md


| #   | 규칙                               | 위반 내용                          | 검증 후 비고               |
| --- | -------------------------------- | ------------------------------ | --------------------- |
| 3   | Controller는 Service만 호출          | 히스토리 API에서 Repository 직접 호출    | **해결** (T-NEW-03)     |
| 6   | 예외 응답은 GlobalExceptionHandler 사용 | SSR은 `SsrExceptionHandler`가 처리 | **완화됨** (M-02 완료와 연계) |


### 1.2 rule-repository.md


| #   | 규칙                      | 위반 내용        | 검증 후 비고      |
| --- | ----------------------- | ------------ | ------------ |
| 5   | 동일 엔티티 중복 Repository 방지 | 다중 모듈에 분산 정의 | **미해결** H-02 |


### 1.3 rule_spring.md


| #   | 규칙                            | 위반 내용                                          | 검증 후 비고                 |
| --- | ----------------------------- | ---------------------------------------------- | ----------------------- |
| 2   | 전역 에러 핸들러 ErrorResponse 포맷 통일 | 주석 vs `Resp` 실제 필드 불일치                         | **부분** → M-01, T-NEW-04 |
| 3   | 비즈니스 예외는 CustomException 사용   | `Reservation` 도메인 메서드에 `IllegalStateException` | **미해결** (다수 라인)         |


### 1.4 rule_test.md


| #   | 규칙                       | 위반 내용                   | 검증 후 비고                            |
| --- | ------------------------ | ----------------------- | ---------------------------------- |
| 1   | 핵심 로직에 대한 테스트 존재         | LLM 핵심 서비스              | **기본 충족** (M-07), 보강은 선택           |
| 6   | TestSecurityConfig 중복 방지 | 다수 WebMvcTest 내부 클래스 반복 | **부분** → M-06 (LLM 5건 통합, 잔여 약 7건) |


---

## 2. Lead 전용 이슈 상세 (리뷰 ID + 상태)

### CRITICAL — 병합 전 반드시 수정


| ID   | 이슈 (요약)            | 위치 / 비고                                                            | 상태     | 수정 방향               |
| ---- | ------------------ | ------------------------------------------------------------------ | ------ | ------------------- |
| C-01 | Jackson·메타데이터 직렬화  | `MedicalService.java` — SB4/Jackson3 정책 재확인 + `toJson` 실패 시 `"{}"` | **진행** | T-NEW-02, 패키지 통일 검토 |
| C-02 | SSR·REST 예외 이중 핸들러 | `GlobalExceptionHandler` + `SsrExceptionHandler`                   | **완화** | 문서화·C-02 종료 조건 합의   |
| C-04 | 히스토리 소유권           | `ChatController`, `MedicalController`                              | **완료** | —                   |


### HIGH


| ID   | 이슈 (요약)                   | 상태      |
| ---- | ------------------------- | ------- |
| H-01 | `Reservation.cancel()` 전이 | **완료**  |
| H-02 | 다중 Repository 통합          | **미착수** |
| H-03 | `resolveStaffId()` 중복     | **완료**  |
| H-06 | LLM 예약 N+1                | **완료**  |
| H-07 | LLM Entity `@Setter`      | **완료**  |


### MEDIUM


| ID   | 이슈 (요약)                | 상태                 |
| ---- | ---------------------- | ------------------ |
| M-01 | 응답 포맷·스키마              | **부분**             |
| M-02 | SSR HTTP/뷰 분기          | **완료**             |
| M-06 | TestSecurityConfig 공통화 | **부분** (LLM 5건 통합) |
| M-07 | LLM 서비스 단위 테스트         | **완료(기본)**         |
| M-09 | `_sample` 정리           | **미착수**            |


### LOW


| ID        | 이슈 (요약)                 | 상태      |
| --------- | ----------------------- | ------- |
| L-01~L-03 | Doctor·Interceptor·enum | **미착수** |


---

## 3. Lead 수정 사항 체크리스트 (우선순위별)

### P0 — 즉시 (남은 CRITICAL 성격)


| ID / TASK       | 작업                                                 | 완료                                                                 |
| --------------- | -------------------------------------------------- | ------------------------------------------------------------------ |
| C-01 / T-NEW-02 | Jackson 정책 문서화 + `toJson` 실패 시 조용한 `"{}"` 제거·예외 정책 | ☑ (`MedicalService.toJson` → `IllegalStateException`, 단위 테스트 추가)   |
| C-02            | 예외 처리 이중 구조를 RULE/AI-CONTEXT에 명시 (SSR vs REST)     | ☑ (`common/exception/AI-CONTEXT.md` + `GlobalExceptionHandler` 주석) |


### P1 — 단기 (HIGH 잔여)


| ID   | 작업                                                    | 완료  |
| ---- | ----------------------------------------------------- | --- |
| H-01 | `cancel()` vs `cancelFully()` 호출 맵·도메인 의도 문서화 및 전이 검증 | ☑   |
| H-02 | Entity별 Repository 통합 설계·단계적 이관                       | ☐   |


### P2 — 중기 (MEDIUM + 신규)


| ID / TASK       | 작업                                               | 완료                                                                             |
| --------------- | ------------------------------------------------ | ------------------------------------------------------------------------------ |
| M-01 / T-NEW-04 | API 오류 응답 필드(errorCode, timestamp, path 등) 계약 정리 | ☐ (T-NEW-04: `Resp` 실제 스키마 주석 반영만 선반영)                                         |
| T-NEW-02        | (상동) `MedicalService` 메타데이터 JSON 실패 처리           | ☑                                                                              |
| T-NEW-03        | LLM 히스토리 조회 Application 서비스 추출                   | ☑ (`ChatService.getRuleHistory`, `MedicalService.getMedicalHistory`)           |
| M-06            | 공통 `TestSecurityConfig`로 12건 중복 축소               | 부분 ☑ (`LlmWebMvcTestSecurityConfig` 5건)                                        |
| M-09            | `_sample` → `@Profile("dev")` 또는 테스트/별 모듈        | 부분 ☑ (`SampleReservationRepository`에 `@Profile("dev")` 추가, Entity DDL은 기존과 동일) |


### P3 — 저우선순위 (LOW + 정리)


| ID / TASK | 작업                                | 완료                                        |
| --------- | --------------------------------- | ----------------------------------------- |
| L-01~L-03 | 리뷰 원문 개선 항목                       | ☐                                         |
| T-NEW-01  | LLM 컨트롤러 미사용 `StaffRepository` 제거 | ☑ (`ChatController`, `MedicalController`) |


### 도메인·예외 (rule_spring #3 연계)


| 작업  | 내용                                                                            | 완료  |
| --- | ----------------------------------------------------------------------------- | --- |
| —   | `Reservation`의 `IllegalStateException` → `CustomException` 또는 도메인 예외 타입 정책 통일 | ☐   |


---

## 4. 참조

- **원본 리뷰:** `doc/CODE_REVIEW_2026-03-20.md`
- **로컬 규칙:** `doc/RULE.md`, `doc/PROJECT_STRUCTURE.md`
- **외부 규칙:** `doc/rules/` 6종
- **빌드 기준:** Spring Boot `4.0.3`, Java 21 (`build.gradle`)

---

## 5. 변경 이력


| 일자         | 내용                                                                                                                                                                                                                                                   |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-03-21 | 저장소 전체 스캔 기반 §0 검증 요약, 완료/미착수 재분류, 신규 TASK 4건(T-NEW-01~04), Repository·TestSecurityConfig 인벤토리 추가                                                                                                                                                    |
| 2026-03-21 | T-NEW-01~03, C-02, T-NEW-04(주석), C-01/T-NEW-02(toJson), M-09(Repository `@Profile`) 구현. 테스트: `MedicalServiceTest`, `ChatServiceHistoryTest`. 부수: `DoctorTreatmentServiceTest` Mock 정합, `ReservationControllerTest` 검증 메시지 정합. `.\gradlew test` 전체 통과 |
| 2026-03-21 | **H-01:** `Reservation.cancel` IN_TREATMENT 가드, 엔티티·`ReceptionService` 문서/예외 매핑, `ReservationTest`. **M-06:** `LlmWebMvcTestSecurityConfig` + LLM WebMvcTest 5곳 `@Import`. `.\gradlew test` 전체 통과                                                    |


