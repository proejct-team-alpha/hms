# 개발자 A (강태오) — SKILL.md

> **역할:** 비회원 메인, 외부 예약 폼, 증상 입력 UI, 추천 결과 화면
> **담당 URL:** `/`, `/reservation/`**
> **담당 화면:** S00 (비회원 메인), S01~S04 (예약 흐름 전체)
> **기준 문서:** PRD v1.0 / 프로젝트 계획서 v4.3 / 화면 기능 정의서 v5.1

---

## 1. 소유 영역

### 1.1 코드 소유권


| 영역             | 경로                                       | 설명                         |
| -------------- | ---------------------------------------- | -------------------------- |
| **Controller** | `reservation/ReservationController.java` | 예약 폼 GET/POST, AJAX 엔드포인트  |
| **Controller** | `home/HomeController.java`               | 메인 페이지                     |
| **Service**    | `reservation/ReservationService.java`    | 예약 생성 비즈니스 로직              |
| **Repository** | `reservation/ReservationRepository.java` | 예약 데이터 접근                  |
| **Repository** | `reservation/PatientRepository.java`     | 환자 findOrCreate            |
| **DTO**        | `reservation/dto/`**                     | ReservationCreateRequest 등 |
| **Template**   | `templates/home/`**                      | 메인 페이지                     |
| **Template**   | `templates/reservation/`**               | 예약 흐름 전체 화면                |
| **Static**     | `static/js/reservation/`**               | 예약 관련 JS                   |


### 1.2 담당 레이아웃

- **L1** (공개): header-public → main → footer-public
- 사이드바 없음

### 1.3 Git 브랜치

```
feature/home
feature/reservation
feature/llm-symptom-ui
```

---

## 2. 관련 PRD 기능


| PRD 기능         | 세부 항목            | 역할                        |
| -------------- | ---------------- | ------------------------- |
| **F01** 비회원 예약 | F01-1 ~ F01-6 전체 | 전담 구현                     |
| **F06** LLM 통합 | F06-1 증상 분석      | UI 연동 (LlmService는 책임개발자) |


---

## 3. 주차별 작업 상세

### W1 — 홈 화면 & 기본 UI 구성

**목표:** 비회원 메인 화면 완성, 공통 레이아웃 적용 확인
**의존:** 책임개발자(김민구)의 Entity·LayoutModelInterceptor 완성 (W1 후반)

#### W1 전반 (Day 1~2)


| #   | 작업                   | 산출물                             | 완료 기준           |
| --- | -------------------- | ------------------------------- | --------------- |
| 1   | 비회원 메인(S00) 화면 구조 설계 | 화면 레이아웃 초안                      | 진료과 안내 영역 구성 확인 |
| 2   | `HomeController` 구현  | `GET /` → `home/index.mustache` | 메인 페이지 렌더링 성공   |
| 3   | 메인 화면 Mustache 작성    | 진료과 목록 동적 표시                    | 예약 버튼 동작        |


#### W1 후반 (Day 3~5)


| #   | 작업            | 산출물          | 완료 기준                     |
| --- | ------------- | ------------ | ------------------------- |
| 4   | 공통 레이아웃 적용 확인 | L1 (헤더 + 푸터) | 정상 렌더링                    |
| 5   | 기본 UI 구성 보조   | CSS 스타일 기초   | 반응형 기본 구조 확인              |
| 6   | 예약 진입점 구성     | 메인 → 예약 라우팅  | `AI 추천 받기` / `직접 선택하기` 분기 |


#### W1 체크포인트

- 메인 페이지 정상 렌더링
- 진료과 목록 DB 조회 후 표시
- 예약 진입 버튼 라우팅 동작

---

### W2 — 외부 예약 폼 전체 완성

**목표:** 직접 선택 예약 전체 흐름 완성 (폼 → 슬롯 선택 → 생성 → 완료)
**의존:** 책임개발자(김민구)의 SlotService (W1에 develop 머지)

#### W2 전반 (Day 1~3)


| #   | 작업                         | 산출물                         | 완료 기준                                |
| --- | -------------------------- | --------------------------- | ------------------------------------ |
| 1   | 예약 폼 화면(S01) 구현            | `reservation/form.mustache` | 환자 정보 입력 폼                           |
| 2   | 진료과 → 의사 동적 조회             | JS 비동기 호출                   | 진료과 선택 시 해당 의사 목록 로드                 |
| 3   | 시간 슬롯 선택(S02) 구현           | `reservation/slot.mustache` | `SlotService.getAvailableSlots()` 호출 |
| 4   | `ReservationController` 구현 | GET/POST `/reservation/`**  | PRG 패턴                               |


#### W2 후반 (Day 4~5)


| #   | 작업                      | 산출물                                  | 완료 기준                              |
| --- | ----------------------- | ------------------------------------ | ---------------------------------- |
| 5   | `ReservationService` 구현 | 예약 생성 비즈니스 로직                        | `SlotService.validateAndLock()` 호출 |
| 6   | 예약번호 발급 & 완료 화면(S04)    | `reservation/complete.mustache`      | 예약번호 표시                            |
| 7   | `PatientRepository` 구현  | Patient findOrCreate                 | 기존 환자 조회 또는 신규 생성                  |
| 8   | @Valid 유효성 검증           | `ReservationCreateRequest` DTO       | 빈 이름·전화번호 시 에러 메시지                 |
| 9   | 단위 테스트 작성               | Service/Repository/Controller 테스트    | 전체 통과                              |
| 10  | PR 제출 & 리뷰 반영           | `feature/reservation` → `develop` PR | 책임개발자 리뷰 승인                        |


#### W2 테스트


| 테스트                         | 유형           | 검증 내용                    |
| --------------------------- | ------------ | ------------------------ |
| `ReservationServiceTest`    | 단위 (Mockito) | 예약 생성 성공, 중복 예약 예외       |
| `ReservationRepositoryTest` | @DataJpaTest | 의사+날짜 조회, 페이징, UNIQUE 제약 |
| `PatientRepositoryTest`     | @DataJpaTest | 전화번호 기반 조회, 환자 저장        |
| `ReservationControllerTest` | MockMvc      | PRG 패턴, @Valid 검증 실패     |


#### W2 체크포인트

- 직접 선택 예약 전체 흐름 동작 (폼 → 슬롯 → 생성 → 완료)
- 중복 예약 방지 동작 확인
- 예약번호 정상 발급
- @Valid 유효성 검증 에러 메시지 표시
- 단위 테스트 전체 통과
- PR 리뷰 승인 & develop 머지

---

### W3 — 증상 입력 UI 골격 구현

**목표:** LLM 연동 없이 증상 입력 화면 골격 완성 (더미 데이터로 UI 확인)
**의존:** 없음 (LlmService 완성 전 독립 작업 가능)

#### W3 전반 (Day 1~3)


| #   | 작업               | 산출물                            | 완료 기준                                  |
| --- | ---------------- | ------------------------------ | -------------------------------------- |
| 1   | 증상 입력 화면(S01) 구현 | `reservation/symptom.mustache` | 텍스트 입력 폼 + AI 추천 버튼                    |
| 2   | 증상 입력 JS 비동기 처리  | fetch 호출 골격                    | 더미 응답으로 `POST /llm/symptom/analyze` 구조 |
| 3   | 추천 결과 영역 UI      | 진료과·의사·시간 추천 렌더링 구조            | 더미 데이터로 UI 확인                          |


#### W3 후반 (Day 4~5)


| #   | 작업                       | 산출물                            | 완료 기준                  |
| --- | ------------------------ | ------------------------------ | ---------------------- |
| 4   | 추천 → 예약 폼 연결 구조          | 쿼리 파라미터 전달                     | 추천 결과 선택 시 예약 폼 자동 입력  |
| 5   | 폴백 UI 구현                 | 오류 toast + 직접 선택 전환            | API 실패 시 3초 후 화면 이동    |
| 6   | 면책 고지 문구 구성              | UI 컴포넌트                        | "AI 참고용, 의학적 진단 아님" 문구 |
| 7   | **W3 금요일 LLM DTO 미팅 참석** | SymptomRequest/Response DTO 확인 | W4 UI 연결 계획 확정         |


#### W3 체크포인트

- 증상 입력 화면 렌더링 (텍스트 입력 + 버튼)
- 추천 결과 영역 더미 데이터로 UI 확인
- 폴백 UI 동작 (toast → 직접 선택 전환)
- 면책 고지 문구 표시
- SymptomRequest/Response DTO 인터페이스 확인 완료

---

### W4 — LLM 연결 완성 & 최종 마무리

**목표:** LlmService 연동으로 증상 추천 기능 완성
**의존:** 책임개발자(김민구)의 LlmService develop 머지 (W4 Day 1) + SymptomRequest/Response DTO

#### W4 전반 (Day 1~3)


| #   | 작업            | 산출물                  | 완료 기준                                   |
| --- | ------------- | -------------------- | --------------------------------------- |
| 1   | LlmService 연결 | fetch 호출 실제 연동       | `POST /llm/symptom/analyze` 실제 추천 결과 수신 |
| 2   | 추천 결과 렌더링 완성  | 추천 진료과·의사·시간 표시      | SymptomResponse DTO 기반 동적 렌더링           |
| 3   | 추천 → 예약 연결 완성 | "이 추천으로 예약하기"        | 예약 폼 자동 채움                              |
| 4   | CSRF 토큰 처리    | `fetchWithCsrf()` 호출 | AJAX POST CSRF 전달                       |


#### W4 후반 (Day 4~5)


| #   | 작업             | 산출물                                  | 완료 기준                 |
| --- | -------------- | ------------------------------------ | --------------------- |
| 5   | 면책 고지 문구 최종 확인 | 위치·스타일                               | 추천 결과 화면에 항상 표시       |
| 6   | 폴백 동작 최종 검증    | API 실패 시나리오                          | 타임아웃·서버 에러 시 직접 선택 전환 |
| 7   | 버그 수정 & UI 정리  | 최종 코드 정리                             | 전체 예약 흐름 재검증          |
| 8   | PR 제출 & 리뷰 반영  | `feature/llm-symptom-ui` → `develop` | 책임개발자 리뷰 승인           |
| 9   | 배포 지원          | 운영 서버 확인                             | 예약 흐름 동작 확인           |


#### W4 체크포인트

- 증상 입력 → AI 추천 → 예약 전체 흐름 동작
- 폴백 시나리오 (타임아웃, 서버 에러) 정상 동작
- 면책 고지 문구 표시 확인
- CSRF 토큰 AJAX 처리 동작
- PR 리뷰 승인 & develop 머지
- 운영 서버 동작 확인

---

## 4. AJAX 엔드포인트 (담당)


| 메서드  | 경로                                      | 설명                             |
| ---- | --------------------------------------- | ------------------------------ |
| GET  | `/reservation/getDoctors?departmentId=` | 진료과별 의사 목록                     |
| GET  | `/reservation/getSlots?doctorId=&date=` | 의사별 가용 시간 슬롯                   |
| POST | `/llm/symptom/analyze`                  | 증상 분석 (UI 연동만, Service는 책임개발자) |


---

## 5. 비즈니스 규칙 (필수 준수)


| 규칙          | 설명                                                 |
| ----------- | -------------------------------------------------- |
| 예약번호 형식     | `RES-YYYYMMDD-NNN` (ReservationNumberGenerator 사용) |
| 중복 예약 방지    | 동일 의사/날짜/시간 UNIQUE 제약                              |
| 진료 가능 요일 검증 | `available_days` 체크 (SlotService에 위임)              |
| 30분 슬롯      | 09:00~17:30                                        |
| LLM 타임아웃    | 5초 → 수동 선택 폴백                                      |
| 면책 조항       | 추천 결과에 반드시 표시                                      |


---

## 6. 절대 터치 금지 영역


| 파일/디렉터리                             | 소유자        | 접근 수준              |
| ----------------------------------- | ---------- | ------------------ |
| `config/SecurityConfig.java`        | 책임개발자(김민구) | 읽기 전용              |
| `domain/*.java` (Entity)            | 책임개발자(김민구) | 접근 금지 (수정 시 Issue) |
| `common/service/SlotService.java`   | 책임개발자(김민구) | 인터페이스 호출만          |
| `llm/LlmService.java`               | 책임개발자(김민구) | 인터페이스 호출만          |
| `staff/`**, `doctor/**`, `nurse/**` | 개발자 B(조유지) | 접근 금지              |
| `admin/**`                          | 개발자 C(강상민) | 접근 금지              |


