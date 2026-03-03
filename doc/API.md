# 🏥 병원 예약 & 내부 업무 시스템 — API 명세서

> **문서 버전:** v5.0
> **작성일:** 2026년
> **변경 이력:** v2.1 (3개 분권, 61개) → v3.0 (단일 문서, 컨트롤러 반환 패턴 전면 개정) → v4.0 (PBL 요구사항 대응 — /api/** JSON API 레이어 추가, @Valid + BindingResult 유효성 검증 패턴 추가, 진료과 activate/deactivate API 정합화, available_days 검증 로직 명시) → **v5.0 (스토리보드 정합 — 내 정보관리 API 추가, 필터 파라미터 보강, 관리자 대시보드 직원수 통계 추가, 상태 전이 권한 스토리보드 기준 통일)**
> **연관 문서:** 프로젝트 계획서 v4.2 / ERD v2.0 / 화면 정의서 v1.2
> **기준:** Spring Boot SSR (Mustache) + RPC 스타일 계층형 URL + JSON API 레이어 (/api/**)
> **인증 방식:** 세션 기반 (Spring Security)

---

## 목차

1. [공통 규칙](#1-공통-규칙)
2. [인증 API](#2-인증-api)
3. [외부 예약 API](#3-외부-예약-api)
4. [LLM 증상 추천 API](#4-llm-증상-추천-api)
5. [접수 직원 API (ROLE_STAFF)](#5-접수-직원-api-role_staff)
6. [의사 API (ROLE_DOCTOR)](#6-의사-api-role_doctor)
7. [간호사 API (ROLE_NURSE)](#7-간호사-api-role_nurse)
8. [LLM 규칙 챗봇 API](#8-llm-규칙-챗봇-api)
9. [관리자 — 예약·환자 API (ROLE_ADMIN)](#9-관리자--예약환자-api-role_admin)
10. [관리자 — 인사 관리 API (ROLE_ADMIN)](#10-관리자--인사-관리-api-role_admin)
11. [관리자 — 진료과 API (ROLE_ADMIN)](#11-관리자--진료과-api-role_admin)
12. [관리자 — 물품 관리 API (ROLE_ADMIN)](#12-관리자--물품-관리-api-role_admin)
13. [관리자 — 병원 규칙 API (ROLE_ADMIN)](#13-관리자--병원-규칙-api-role_admin)
14. [관리자 — 대시보드 API (ROLE_ADMIN)](#14-관리자--대시보드-api-role_admin)
15. [JSON API 레이어 (/api/**)](#15-json-api-레이어-api)
16. [에러 코드 정의](#16-에러-코드-정의)
17. [전체 API 목록 요약](#17-전체-api-목록-요약)

---

## 1. 공통 규칙

### 1.1 Base URL

```
http://{host}:{port}
```

### 1.2 URL 설계 원칙 (RPC 스타일)

| 원칙 | 내용 |
|------|------|
| **메서드** | `GET` (조회·화면 렌더링), `POST` (생성·변경·액션) 만 사용 |
| **URL 구조** | `/{역할}/{자원}/{액션}` 계층형 구조 |
| **ID 전달** | Path Variable 사용 안함. Query Parameter 또는 Request Body로 전달 |
| **액션 명시** | URL에 동사를 명시 (`/create`, `/update`, `/delete`, `/deactivate` 등) |
| **일관성** | 화면 렌더링(GET)과 처리(POST)는 같은 경로 prefix 공유 |

| 메서드 | URL 예시 | 설명 |
|--------|----------|------|
| `GET` | `/staff/reception/list` | 목록 화면 렌더링 |
| `GET` | `/staff/reception/detail` | 상세 화면 렌더링 |
| `GET` | `/admin/staff/new` | 등록 폼 화면 렌더링 |
| `POST` | `/staff/reception/receive` | 접수 처리 |
| `POST` | `/admin/department/create` | 진료과 등록 |
| `POST` | `/admin/rule/toggleActive` | 규칙 활성화 토글 |

### 1.3 URL 접두어별 접근 권한

| 접두어 | 대상 | 인증 |
|--------|------|------|
| `/reservation/**` | 외부 비회원 환자 | 불필요 |
| `/staff/**` | ROLE_STAFF | 세션 필요 |
| `/doctor/**` | ROLE_DOCTOR | 세션 필요 |
| `/nurse/**` | ROLE_NURSE | 세션 필요 |
| `/admin/**` | ROLE_ADMIN | 세션 필요 |
| `/api/**` | RESTful API (역할별 인증) | 세션 필요 |
| `/llm/**` | LLM 비동기 호출 | 세션 필요 (DOCTOR, NURSE) / 불필요 (환자 증상) |

### 1.4 HTTP 메서드 규칙

| 메서드 | 용도 |
|--------|------|
| `GET` | 화면 렌더링 (Mustache SSR) 또는 비동기 데이터 조회 (AJAX) |
| `POST` | 데이터 생성 / 상태 변경 / 삭제 / LLM 호출 등 모든 액션 (`/api/**` JSON API 포함) |

### 1.5 컨트롤러 반환 규칙 (v3.0 핵심 변경)

v3.0부터 모든 컨트롤러는 아래 패턴을 따릅니다. JSON 응답 바디(`{success, data, message}`)는 **LLM 엔드포인트와 비동기 조회 API(getSlots·getDoctors), 그리고 `/api/**` JSON API 레이어에만** 유지합니다.

#### GET — 화면 렌더링

```java
// 예시
@GetMapping("/staff/reception/list")
public String receptionList(HttpServletRequest request) {
    request.setAttribute("reservations", service.getTodayList());
    request.setAttribute("today", LocalDate.now().toString());
    return "staff/reception/list";   // Mustache 뷰 경로
}
```

| 항목 | 내용 |
|------|------|
| **반환값** | Mustache 뷰 경로 문자열 (`"역할/자원/액션"`) |
| **데이터 전달** | `HttpServletRequest.setAttribute(key, value)` 또는 `Model.addAttribute(key, value)` |
| **인증 오류** | Spring Security가 자동으로 `/login` 리다이렉트 처리 |

#### POST — 성공 처리 (PRG 패턴)

```java
// 예시
@PostMapping("/staff/reception/receive")
public String receive(@RequestBody ReceiveRequest req,
                      RedirectAttributes ra) {
    service.receive(req);
    ra.addFlashAttribute("successMessage", "접수가 완료되었습니다.");
    ra.addFlashAttribute("reservationId", req.getReservationId());
    return "redirect:/staff/reception/list";
}
```

| 항목 | 내용 |
|------|------|
| **반환값** | `"redirect:/다음화면경로"` (Post-Redirect-Get 패턴) |
| **성공 메시지** | `RedirectAttributes.addFlashAttribute("successMessage", "...")` |
| **성공 데이터** | `RedirectAttributes.addFlashAttribute("키", 값)` (화면에 필요한 경우) |

#### POST — 오류 처리 (폼 재렌더링)

```java
// 예시
@PostMapping("/staff/reception/receive")
public String receive(@RequestBody ReceiveRequest req,
                      HttpServletRequest request) {
    try {
        service.receive(req);
        // ... redirect
    } catch (InvalidStatusTransitionException e) {
        request.setAttribute("errorCode", "INVALID_STATUS_TRANSITION");
        request.setAttribute("errorMessage", "RESERVED 상태에서만 접수 가능합니다.");
        request.setAttribute("reservation", service.getDetail(req.getReservationId()));
        return "staff/reception/detail";   // 폼 뷰 재반환
    }
}
```

| 항목 | 내용 |
|------|------|
| **반환값** | 원래 폼 뷰 경로 (폼 재렌더링) |
| **오류 코드** | `request.setAttribute("errorCode", "에러코드상수")` |
| **오류 메시지** | `request.setAttribute("errorMessage", "사용자 표시 메시지")` |
| **입력값 보존** | `request.setAttribute("inputData", 입력폼DTO)` (UX를 위해 입력값 복원) |

#### AJAX 엔드포인트 (JSON 유지)

아래 엔드포인트는 비동기 호출이므로 JSON `@ResponseBody` 응답을 유지합니다.

| URL | 용도 |
|-----|------|
| `POST /llm/symptom/analyze` | LLM 증상 분석 (비동기) |
| `POST /llm/rules/ask` | LLM 규칙 챗봇 질의 (비동기) |
| `GET /llm/rules/history` | 챗봇 이력 조회 (비동기) |
| `GET /reservation/getDoctors` | 진료과별 의사 목록 (폼 동적 갱신) |
| `GET /reservation/getSlots` | 예약 가능 시간 슬롯 (폼 동적 갱신) |
| `GET /admin/dashboard/stats` | 대시보드 통계 (비동기 갱신) |

**AJAX 성공 응답 형식**
```json
{
  "success": true,
  "data": { },
  "message": "처리 완료"
}
```

**AJAX 실패 응답 형식**
```json
{
  "success": false,
  "errorCode": "에러코드",
  "message": "사용자 표시 메시지"
}
```

### 1.6 JSON API 레이어 (/api/**)

PBL 최소 요구사항 충족을 위해, 기존 SSR PRG 패턴 외에 별도 `/api/**` JSON API 레이어를 추가한다. 이 레이어는 POST 메서드를 사용하며 JSON 응답을 반환한다.

| 메서드 | URL | 설명 | 인증 |
|--------|-----|------|------|
| `POST` | `/api/staff/{id}/update` | 직원 정보 수정 | ROLE_ADMIN |
| `POST` | `/api/patients/{id}/update` | 환자 정보 수정 | ROLE_NURSE, ROLE_ADMIN |
| `POST` | `/api/reservations/{id}/cancel` | 예약 취소 | ROLE_ADMIN |
| `POST` | `/api/items/{id}/delete` | 물품 삭제 | ROLE_ADMIN |
| `POST` | `/api/rules/{id}/delete` | 규칙 삭제 | ROLE_ADMIN |

> 상세 요청/응답은 [15. JSON API 레이어](#15-json-api-레이어-api) 참조.

### 1.7 서버 사이드 유효성 검증 패턴

모든 POST 엔드포인트에서 `@Valid` + `BindingResult`를 사용한 서버 사이드 유효성 검증을 수행한다.

**검증 실패 시 처리 패턴:**

| 엔드포인트 유형 | 처리 방식 |
|----------------|-----------|
| **SSR 엔드포인트** | 폼 뷰 재렌더링 + `request.setAttribute("errors", bindingResult.getAllErrors())` + `request.setAttribute("inputData", request)` |
| **REST API 엔드포인트** | 400 응답 + `{ "success": false, "errorCode": "VALIDATION_ERROR", "message": "...", "errors": [{"field": "...", "message": "..."}] }` |

**SSR 유효성 검증 예시:**

```java
@PostMapping("/staff/reservation/create")
public String create(@Valid ReservationCreateRequest req,
                     BindingResult bindingResult,
                     HttpServletRequest request) {
    if (bindingResult.hasErrors()) {
        request.setAttribute("errors", bindingResult.getAllErrors());
        request.setAttribute("inputData", req);
        request.setAttribute("errorCode", "VALIDATION_ERROR");
        request.setAttribute("errorMessage", "입력값을 확인해 주세요.");
        request.setAttribute("departments", departmentService.getActiveList());
        return "staff/reservation/new";
    }
    // ... 비즈니스 로직
}
```

**REST API 유효성 검증 실패 응답 예시:**

```json
{
  "success": false,
  "errorCode": "VALIDATION_ERROR",
  "message": "입력값을 확인해 주세요.",
  "errors": [
    { "field": "patientName", "message": "환자 이름은 필수입니다." },
    { "field": "patientPhone", "message": "연락처 형식이 올바르지 않습니다." }
  ]
}
```

**주요 DTO 유효성 규칙:**

| DTO | 필드 | 검증 규칙 |
|-----|------|-----------|
| `ReservationCreateRequest` | `patientName` | `@NotBlank`, `@Size(max=50)` |
| `ReservationCreateRequest` | `patientPhone` | `@NotBlank`, `@Pattern(regexp="^\\d{2,3}-\\d{3,4}-\\d{4}$")` |
| `ReservationCreateRequest` | `departmentId` | `@NotNull` |
| `ReservationCreateRequest` | `doctorId` | `@NotNull` |
| `ReservationCreateRequest` | `reservationDate` | `@NotNull`, `@Future` |
| `ReservationCreateRequest` | `timeSlot` | `@NotBlank`, `@Pattern(regexp="^\\d{2}:\\d{2}$")` |
| `StaffCreateRequest` | `username` | `@NotBlank`, `@Size(min=4, max=20)`, `@Pattern(regexp="^[a-zA-Z0-9]+$")` |
| `StaffCreateRequest` | `password` | `@NotBlank`, `@Size(min=8)` |
| `StaffCreateRequest` | `name` | `@NotBlank`, `@Size(max=50)` |
| `StaffCreateRequest` | `role` | `@NotNull` |
| `ItemCreateRequest` | `name` | `@NotBlank`, `@Size(max=200)` |
| `ItemCreateRequest` | `category` | `@NotNull` |
| `ItemCreateRequest` | `quantity` | `@NotNull`, `@Min(0)` |
| `ItemCreateRequest` | `minQuantity` | `@NotNull`, `@Min(0)` |
| `RuleCreateRequest` | `title` | `@NotBlank`, `@Size(max=200)` |
| `RuleCreateRequest` | `content` | `@NotBlank`, `@Size(max=3000)` |
| `RuleCreateRequest` | `category` | `@NotNull` |

### 1.8 의사 진료 요일 검증 (available_days)

예약 생성 시(`POST /reservation/create`, `POST /staff/reservation/create`, `POST /staff/walkin/create`) SlotService에서 DOCTOR.available_days 기반 요일 검증을 수행한다.

| 항목 | 내용 |
|------|------|
| **검증 시점** | 예약 생성 요청 처리 중 SlotService 호출 시 |
| **검증 로직** | 요청된 `reservationDate`의 요일이 해당 의사의 `available_days`에 포함되는지 확인 |
| **실패 시** | `DOCTOR_NOT_AVAILABLE` 에러 반환 |
| **NULL 처리** | `available_days`가 NULL인 경우 모든 평일(MON~FRI) 진료 가능으로 간주 |

```java
// SlotService 검증 예시
public void validateDoctorAvailability(Long doctorId, LocalDate reservationDate) {
    Doctor doctor = doctorRepository.findById(doctorId)
        .orElseThrow(() -> new ResourceNotFoundException("RESOURCE_NOT_FOUND"));

    List<DayOfWeek> availableDays = doctor.getAvailableDays();
    if (availableDays == null) {
        // NULL이면 평일(MON~FRI) 전체 가능
        availableDays = List.of(MON, TUE, WED, THU, FRI);
    }

    DayOfWeek requestedDay = reservationDate.getDayOfWeek();
    if (!availableDays.contains(requestedDay)) {
        throw new DoctorNotAvailableException("DOCTOR_NOT_AVAILABLE");
    }
}
```

### 1.9 인증 오류 공통 처리

| 상황 | 처리 |
|------|------|
| 미로그인 접근 | Spring Security → `302 /login` 리다이렉트 |
| 권한 없는 접근 | Spring Security → `403` 권한 오류 화면 렌더링 |
| 세션 만료 | Spring Security → `302 /login` 리다이렉트 |

---

## 2. 인증 API

### 2.1 로그인 화면

```
GET /login
```

| 항목 | 내용 |
|------|------|
| 설명 | 로그인 폼 화면 렌더링 |
| 인증 | 불필요 |

**컨트롤러 반환**: `"auth/login"`

---

### 2.2 로그인 처리

```
POST /login
```

| 항목 | 내용 |
|------|------|
| 설명 | username + password 인증 후 세션 발급 — Spring Security 자동 처리 |
| 인증 | 불필요 |

**Request Body (Form)**
```
username=admin01
password=password123
```

**성공 시 리다이렉트** (Spring Security `AuthenticationSuccessHandler`)

| ROLE | 리다이렉트 경로 |
|------|----------------|
| `ROLE_ADMIN` | `/admin/dashboard` |
| `ROLE_DOCTOR` | `/doctor/dashboard` |
| `ROLE_NURSE` | `/nurse/dashboard` |
| `ROLE_STAFF` | `/staff/dashboard` |

**실패 시**: `redirect:/login?error=true`

---

### 2.3 로그아웃

```
POST /logout
```

| 항목 | 내용 |
|------|------|
| 설명 | 세션 무효화 — Spring Security 자동 처리 |
| 인증 | 세션 필요 |

**처리 후**: `redirect:/login?logout=true`

---

## 3. 외부 예약 API

> 비회원 환자 대상. 세션 인증 불필요.

### 3.1 비회원 메인 화면

```
GET /
```

| 항목 | 내용 |
|------|------|
| 설명 | 비회원 메인 화면 렌더링 — 활성 진료과 목록 표시 |
| 인증 | 불필요 |

**컨트롤러 반환**: `"index"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `departments` | `List<DepartmentDto>` | 활성(is_active=TRUE) 진료과 목록 |

---

### 3.2 예약 방식 선택 화면

```
GET /reservation
```

| 항목 | 내용 |
|------|------|
| 설명 | AI 추천 예약 / 직접 선택 예약 분기 화면 렌더링 |
| 인증 | 불필요 |

**컨트롤러 반환**: `"reservation/index"`

---

### 3.3 증상 입력 화면

```
GET /reservation/symptom
```

| 항목 | 내용 |
|------|------|
| 설명 | 증상 텍스트 입력 폼 렌더링 |
| 인증 | 불필요 |

**컨트롤러 반환**: `"reservation/symptom"`

---

### 3.4 직접 선택 예약 화면

```
GET /reservation/direct
```

| 항목 | 내용 |
|------|------|
| 설명 | 진료과·의사·날짜·시간 직접 선택 예약 폼 렌더링 |
| 인증 | 불필요 |

**Query Parameters** (LLM 추천 경유 시 자동 입력용)

| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `recommendedDept` | 선택 | LLM 추천 진료과명 |
| `recommendedDoctor` | 선택 | LLM 추천 의사명 |
| `recommendedTime` | 선택 | LLM 추천 시간대 |

**컨트롤러 반환**: `"reservation/direct"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `departments` | `List<DepartmentDto>` | 활성 진료과 목록 |
| `prefilledDept` | `String` | 추천 진료과명 (있을 경우) |
| `prefilledDoctor` | `String` | 추천 의사명 (있을 경우) |
| `prefilledTime` | `String` | 추천 시간대 (있을 경우) |

---

### 3.5 의사별 예약 가능 시간 조회 (AJAX)

```
GET /reservation/getSlots
```

| 항목 | 내용 |
|------|------|
| 설명 | 선택한 의사·날짜 기준 예약 가능 시간 슬롯 조회 — 폼 동적 갱신용 AJAX |
| 인증 | 불필요 |
| 반환 | `@ResponseBody` JSON |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `doctorId` | Long | ✅ | 의사 ID |
| `date` | String | ✅ | 예약 날짜 (yyyy-MM-dd) |

**응답 JSON (성공)**
```json
{
  "success": true,
  "data": {
    "availableSlots": ["09:00", "09:30", "10:00", "11:00"],
    "unavailableSlots": ["10:30", "11:30"]
  }
}
```

---

### 3.6 진료과별 의사 목록 조회 (AJAX)

```
GET /reservation/getDoctors
```

| 항목 | 내용 |
|------|------|
| 설명 | 선택한 진료과에 소속된 의사 목록 조회 — 폼 동적 갱신용 AJAX |
| 인증 | 불필요 |
| 반환 | `@ResponseBody` JSON |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `departmentId` | Long | ✅ | 진료과 ID |
| `date` | String | 선택 | 날짜 지정 시 available_days 기반 필터링 |

**응답 JSON (성공)**
```json
{
  "success": true,
  "data": {
    "doctors": [
      {
        "id": 1,
        "name": "김철수",
        "specialty": "소화기내과",
        "availableDays": ["MON", "WED", "FRI"]
      }
    ]
  }
}
```

---

### 3.7 예약 생성

```
POST /reservation/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 비회원 예약 생성. 중복 체크 후 Patient + Reservation 저장 |
| 인증 | 불필요 |

**Request Body (Form 또는 JSON)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `patientName` | String | ✅ | 환자 성명 |
| `patientPhone` | String | ✅ | 연락처 |
| `patientEmail` | String | 선택 | 이메일 |
| `departmentId` | Long | ✅ | 진료과 ID |
| `doctorId` | Long | ✅ | 의사 ID |
| `reservationDate` | String | ✅ | yyyy-MM-dd |
| `timeSlot` | String | ✅ | HH:mm (30분 단위 슬롯) |
| `llmRecommendationId` | Long | 선택 | LLM 추천 경유 시 → is_used = TRUE 업데이트 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/reservation/complete` |
| Flash | `successMessage` = `"예약이 완료되었습니다."` |
| Flash | `reservationNumber` = `"RES-20260315-001"` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `DUPLICATE_RESERVATION` | `"reservation/direct"` | `errorCode`, `errorMessage`, `inputData` |
| `DOCTOR_NOT_AVAILABLE` | `"reservation/direct"` | `errorCode`, `errorMessage`, `inputData` |
| `VALIDATION_ERROR` | `"reservation/direct"` | `errorCode`, `errorMessage`, `inputData` |

> **available_days 검증:** SlotService에서 요청된 `reservationDate`의 요일이 해당 의사의 `available_days`에 포함되지 않으면 `DOCTOR_NOT_AVAILABLE` 에러를 반환한다. `available_days`가 NULL인 경우 모든 평일(MON~FRI) 진료 가능으로 간주한다.

---

### 3.8 예약 완료 화면

```
GET /reservation/complete
```

| 항목 | 내용 |
|------|------|
| 설명 | 예약 완료 후 예약번호 표시 화면 렌더링 |
| 인증 | 불필요 |

**컨트롤러 반환**: `"reservation/complete"`

**Request Attributes** (Flash 또는 Query Param에서 받음)

| 키 | 타입 | 설명 |
|----|------|------|
| `reservationNumber` | `String` | 발급된 예약번호 |
| `successMessage` | `String` | 완료 메시지 |

---

## 4. LLM 증상 추천 API

### 4.1 증상 분석 및 추천 (AJAX)

```
POST /llm/symptom/analyze
```

| 항목 | 내용 |
|------|------|
| 설명 | 증상 텍스트를 Claude API에 전달하여 진료과·의사·시간 추천 반환 |
| 인증 | 불필요 |
| 처리 방식 | 서버 사이드 Claude API 호출 (API Key 노출 없음) |
| 타임아웃 | 5초 초과 시 폴백 응답 반환 |
| 반환 | `@ResponseBody` JSON |

**Request Body**
```json
{
  "symptomText": "3일 전부터 오른쪽 아랫배가 아프고 미열이 지속됩니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `symptomText` | String | ✅ | 환자 증상 텍스트 (최대 1000자) |

**응답 JSON (성공)**
```json
{
  "success": true,
  "data": {
    "recommendationId": 42,
    "recommendedDepartment": "외과",
    "recommendedDoctor": "김철수",
    "recommendedTime": "오전",
    "summary": "복통과 미열 증상으로 외과 진료가 적합합니다.",
    "disclaimer": "이 결과는 AI 참고 안내이며, 의학적 진단이 아닙니다. 최종 판단은 반드시 의사에게 받으시기 바랍니다."
  }
}
```

| 필드 | 설명 |
|------|------|
| `recommendationId` | LLM_RECOMMENDATION 저장 ID (예약 확정 시 `/reservation/create` 에 전달) |
| `recommendedDepartment` | 추천 진료과명 |
| `recommendedDoctor` | 추천 의사명 |
| `recommendedTime` | 추천 시간대 (오전/오후) |
| `disclaimer` | 면책 고지 문구 (화면 필수 표시) |

**응답 JSON (LLM 실패 — 폴백)**
```json
{
  "success": false,
  "errorCode": "LLM_SERVICE_UNAVAILABLE",
  "message": "AI 추천 서비스가 일시적으로 불가합니다. 직접 선택하여 예약해 주세요."
}
```

> 프론트에서 `LLM_SERVICE_UNAVAILABLE` 수신 시 `/reservation/direct`로 자동 이동.

---

## 5. 접수 직원 API (ROLE_STAFF)

### 5.1 STAFF 대시보드 화면

```
GET /staff/dashboard
```

| 항목 | 내용 |
|------|------|
| 설명 | 접수 직원 대시보드 — 오늘 예약 집계 + 미접수 상위 5건 |
| 인증 | ROLE_STAFF |

**컨트롤러 반환**: `"staff/dashboard"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staffName` | `String` | 로그인 직원 이름 |
| `today` | `String` | 오늘 날짜 (yyyy-MM-dd) |
| `totalCount` | `Integer` | 오늘 전체 예약 수 |
| `reservedCount` | `Integer` | 미접수(RESERVED) 수 |
| `receivedCount` | `Integer` | 접수완료(RECEIVED) 수 |
| `upcomingReservations` | `List<ReceptionDto>` | 미접수 상위 5건 |

---

### 5.2 오늘 접수 목록 화면

```
GET /staff/reception/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 당일 RESERVED 상태 예약 목록 렌더링 |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `status` | String | 선택 | 상태 필터 (RESERVED/RECEIVED/COMPLETED/CANCELLED) |
| `departmentId` | Long | 선택 | 진료과 필터 |
| `keyword` | String | 선택 | 환자명 검색 (부분 일치) |
| `page` | Integer | 선택 | 페이지 번호 (기본값: 0) |
| `size` | Integer | 선택 | 페이지 크기 (기본값: 20) |

**컨트롤러 반환**: `"staff/reception/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `reservations` | `List<ReservationDto>` | 오늘 예약 목록 |
| `today` | `String` | 오늘 날짜 |
| `totalCount` | `Integer` | 전체 건수 |
| `successMessage` | `String` | Flash — 접수 완료 후 리다이렉트 시 메시지 |

---

### 5.3 접수 처리 화면

```
GET /staff/reception/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 특정 예약의 접수 처리 폼 렌더링 (환자 추가 정보 입력) |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `reservationId` | Long | ✅ | 예약 ID |

**컨트롤러 반환**: `"staff/reception/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `reservation` | `ReservationDto` | 예약 정보 |
| `patient` | `PatientDto` | 환자 기본 정보 |
| `errorCode` | `String` | Flash — 오류 시 코드 |
| `errorMessage` | `String` | Flash — 오류 시 메시지 |

---

### 5.4 접수 처리

```
POST /staff/reception/receive
```

| 항목 | 내용 |
|------|------|
| 설명 | 환자 추가 정보 업데이트 + 예약 상태 RESERVED → RECEIVED |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reservationId` | Long | ✅ | 예약 ID |
| `address` | String | 선택 | 환자 주소 |
| `note` | String | 선택 | 특이사항 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/staff/reception/list` |
| Flash | `successMessage` = `"접수가 완료되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `INVALID_STATUS_TRANSITION` | `"staff/reception/detail"` | `errorCode`, `errorMessage`, `reservation`, `patient` |
| `RESERVATION_NOT_FOUND` | `"staff/reception/detail"` | `errorCode`, `errorMessage` |

---

### 5.5 전화 예약 등록 화면

```
GET /staff/reservation/new
```

| 항목 | 내용 |
|------|------|
| 설명 | 창구에서 전화 예약을 직접 등록하는 폼 렌더링 |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**컨트롤러 반환**: `"staff/reservation/new"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `departments` | `List<DepartmentDto>` | 활성 진료과 목록 |
| `errorCode` | `String` | Flash — 등록 실패 시 코드 |
| `errorMessage` | `String` | Flash — 등록 실패 시 메시지 |
| `inputData` | `ReservationFormDto` | Flash — 입력값 복원용 |

---

### 5.6 전화 예약 등록 처리

```
POST /staff/reservation/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 접수 직원이 환자 대신 예약 생성 (초기 상태: RESERVED) |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `patientName` | String | ✅ | 환자 이름 |
| `patientPhone` | String | ✅ | 연락처 |
| `patientEmail` | String | 선택 | 이메일 |
| `departmentId` | Long | ✅ | 진료과 ID |
| `doctorId` | Long | ✅ | 의사 ID |
| `reservationDate` | String | ✅ | yyyy-MM-dd |
| `timeSlot` | String | ✅ | HH:mm |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/staff/reception/list` |
| Flash | `successMessage` = `"전화 예약이 등록되었습니다."` |
| Flash | `reservationNumber` = 생성된 예약번호 |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `DUPLICATE_RESERVATION` | `"staff/reservation/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |
| `DOCTOR_NOT_AVAILABLE` | `"staff/reservation/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |
| `VALIDATION_ERROR` | `"staff/reservation/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |

> **available_days 검증:** SlotService에서 요청된 `reservationDate`의 요일이 해당 의사의 `available_days`에 포함되지 않으면 `DOCTOR_NOT_AVAILABLE` 에러를 반환한다. `available_days`가 NULL인 경우 모든 평일(MON~FRI) 진료 가능으로 간주한다.

---

### 5.7 방문 접수 화면

```
GET /staff/walkin/new
```

| 항목 | 내용 |
|------|------|
| 설명 | 방문 접수 폼 렌더링 — 오늘 날짜 기본값, 진료과·의사 선택 |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**컨트롤러 반환**: `"staff/walkin/new"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `departments` | `List<DepartmentDto>` | 활성 진료과 목록 |
| `today` | `String` | 오늘 날짜 (기본값 자동 입력용) |
| `errorCode` | `String` | Flash — 오류 시 코드 |
| `errorMessage` | `String` | Flash — 오류 시 메시지 |
| `inputData` | `WalkinFormDto` | Flash — 입력값 복원용 |

---

### 5.8 방문 접수 등록 처리

```
POST /staff/walkin/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 방문 환자 Patient 생성 + Reservation 생성 (status=RECEIVED, source=WALKIN), 단일 `@Transactional` |
| 인증 | ROLE_STAFF, ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `patientName` | String | ✅ | 환자 이름 |
| `patientPhone` | String | ✅ | 연락처 |
| `patientEmail` | String | 선택 | 이메일 |
| `address` | String | 선택 | 주소 |
| `note` | String | 선택 | 특이사항 |
| `departmentId` | Long | ✅ | 진료과 ID |
| `doctorId` | Long | ✅ | 의사 ID |
| `reservationDate` | String | ✅ | yyyy-MM-dd (기본: 오늘) |
| `timeSlot` | String | ✅ | HH:mm |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/staff/reception/list` |
| Flash | `successMessage` = `"방문 접수가 완료되었습니다."` |
| Flash | `reservationNumber` = 생성된 예약번호 |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `DUPLICATE_RESERVATION` | `"staff/walkin/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |
| `DOCTOR_NOT_AVAILABLE` | `"staff/walkin/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |
| `VALIDATION_ERROR` | `"staff/walkin/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |

> **available_days 검증:** SlotService에서 요청된 `reservationDate`의 요일이 해당 의사의 `available_days`에 포함되지 않으면 `DOCTOR_NOT_AVAILABLE` 에러를 반환한다. `available_days`가 NULL인 경우 모든 평일(MON~FRI) 진료 가능으로 간주한다.

---

### 5.9 STAFF 내 정보관리 화면

```
GET /staff/mypage
```

| 항목 | 내용 |
|------|------|
| 설명 | 접수 직원 내 정보 관리 화면 렌더링 |
| 인증 | ROLE_STAFF |

**컨트롤러 반환**: `"staff/mypage"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staff` | `StaffDto` | 로그인한 직원 정보 (이름, 사원번호, 담당업무 읽기전용 + 이메일, 연락처 수정가능) |
| `successMessage` | `String` | Flash — 수정 완료 메시지 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 5.10 STAFF 내 정보 수정 처리

```
POST /staff/mypage/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 이메일·연락처 수정 또는 비밀번호 변경 |
| 인증 | ROLE_STAFF |

**Request Body (Form)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | 선택 | 이메일 |
| `phone` | String | 선택 | 연락처 |
| `currentPassword` | String | 선택 | 현재 비밀번호 (비밀번호 변경 시 필수) |
| `newPassword` | String | 선택 | 새 비밀번호 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/staff/mypage` |
| Flash | `successMessage` = `"정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `INVALID_PASSWORD` | `"staff/mypage"` | `errorCode`, `errorMessage`, `staff` |
| `VALIDATION_ERROR` | `"staff/mypage"` | `errorCode`, `errorMessage`, `staff` |

---

## 6. 의사 API (ROLE_DOCTOR)

### 6.1 DOCTOR 대시보드 화면

```
GET /doctor/dashboard
```

| 항목 | 내용 |
|------|------|
| 설명 | 의사 대시보드 — 오늘 진료 집계 + 진료 대기 상위 3건 |
| 인증 | ROLE_DOCTOR |

**컨트롤러 반환**: `"doctor/dashboard"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `doctorName` | `String` | 로그인 의사 이름 |
| `departmentName` | `String` | 소속 진료과명 |
| `today` | `String` | 오늘 날짜 |
| `totalCount` | `Integer` | 오늘 전체 진료 수 |
| `waitingCount` | `Integer` | 진료 대기(RECEIVED) 수 |
| `completedCount` | `Integer` | 완료(COMPLETED) 수 |
| `upcomingTreatments` | `List<TreatmentDto>` | 진료 대기 상위 3건 |

---

### 6.2 오늘 진료 목록 화면

```
GET /doctor/treatment/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 로그인 의사의 당일 RECEIVED 상태 환자 목록 렌더링 |
| 인증 | ROLE_DOCTOR, ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `status` | String | 선택 | 상태 필터 (RECEIVED/COMPLETED) |
| `keyword` | String | 선택 | 환자명 검색 (부분 일치) |

**컨트롤러 반환**: `"doctor/treatment/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `treatments` | `List<TreatmentDto>` | 오늘 진료 대기 목록 |
| `doctorName` | `String` | 의사 이름 |
| `today` | `String` | 오늘 날짜 |
| `successMessage` | `String` | Flash — 진료 완료 후 메시지 |

---

### 6.3 진료 기록 입력 화면

```
GET /doctor/treatment/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 특정 환자의 진료 기록 입력 폼 렌더링 |
| 인증 | ROLE_DOCTOR (본인 담당 환자만), ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `reservationId` | Long | ✅ | 예약 ID |

**컨트롤러 반환**: `"doctor/treatment/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `reservation` | `ReservationDto` | 예약 정보 |
| `patient` | `PatientDto` | 환자 정보 (이름, 연락처, 특이사항 등) |
| `errorCode` | `String` | Flash — 오류 시 코드 |
| `errorMessage` | `String` | Flash — 오류 시 메시지 |

---

### 6.4 진료 완료 처리

```
POST /doctor/treatment/complete
```

| 항목 | 내용 |
|------|------|
| 설명 | 진료 기록 저장 + 예약 상태 RECEIVED → COMPLETED (단일 트랜잭션) |
| 인증 | ROLE_DOCTOR (본인 담당 환자만), ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reservationId` | Long | ✅ | 예약 ID |
| `diagnosisNote` | String | 선택 | 진료 내용 (증상, 처방, 소견 등) |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/doctor/treatment/list` |
| Flash | `successMessage` = `"진료가 완료 처리되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `INVALID_STATUS_TRANSITION` | `"doctor/treatment/detail"` | `errorCode`, `errorMessage`, `reservation`, `patient` |
| `NOT_OWN_PATIENT` | `"doctor/treatment/list"` | `errorCode`, `errorMessage` |
| `RESERVATION_NOT_FOUND` | `"doctor/treatment/list"` | `errorCode`, `errorMessage` |

---

### 6.5 DOCTOR 내 정보관리 화면

```
GET /doctor/mypage
```

| 항목 | 내용 |
|------|------|
| 설명 | 의사 내 정보 관리 화면 렌더링 |
| 인증 | ROLE_DOCTOR |

**컨트롤러 반환**: `"doctor/mypage"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staff` | `StaffDto` | 로그인한 직원 정보 (이름, 사원번호, 전문과 읽기전용 + 이메일, 연락처 수정가능) |
| `successMessage` | `String` | Flash — 수정 완료 메시지 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 6.6 DOCTOR 내 정보 수정 처리

```
POST /doctor/mypage/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 이메일·연락처 수정 또는 비밀번호 변경 |
| 인증 | ROLE_DOCTOR |

**Request Body (Form)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | 선택 | 이메일 |
| `phone` | String | 선택 | 연락처 |
| `currentPassword` | String | 선택 | 현재 비밀번호 (비밀번호 변경 시 필수) |
| `newPassword` | String | 선택 | 새 비밀번호 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/doctor/mypage` |
| Flash | `successMessage` = `"정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `INVALID_PASSWORD` | `"doctor/mypage"` | `errorCode`, `errorMessage`, `staff` |
| `VALIDATION_ERROR` | `"doctor/mypage"` | `errorCode`, `errorMessage`, `staff` |

---

## 7. 간호사 API (ROLE_NURSE)

### 7.1 NURSE 대시보드 화면

```
GET /nurse/dashboard
```

| 항목 | 내용 |
|------|------|
| 설명 | 간호사 대시보드 — 오늘 상태별 집계 + 진료 대기 상위 3건 + 재고 부족 알림 |
| 인증 | ROLE_NURSE |

**컨트롤러 반환**: `"nurse/dashboard"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `nurseName` | `String` | 로그인 간호사 이름 |
| `today` | `String` | 오늘 날짜 |
| `summary` | `ScheduleSummaryDto` | 상태별 집계 (total/reserved/received/completed) |
| `upcomingReceived` | `List<ReservationDto>` | 진료 대기 상위 3건 |
| `shortageItems` | `List<ItemDto>` | 재고 부족 물품 목록 |

---

### 7.2 오늘 예약·접수 현황 화면

```
GET /nurse/schedule/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 당일 전체 예약 현황을 상태별로 분류하여 렌더링 |
| 인증 | ROLE_NURSE, ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `status` | String | 선택 | 상태 필터 |
| `departmentId` | Long | 선택 | 진료과 필터 |
| `doctorId` | Long | 선택 | 의사 필터 |
| `keyword` | String | 선택 | 환자명 검색 (부분 일치) |

**컨트롤러 반환**: `"nurse/schedule/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `today` | `String` | 오늘 날짜 |
| `summary` | `ScheduleSummaryDto` | 상태별 집계 |
| `reservations` | `List<ReservationDto>` | 전체 예약 목록 |

---

### 7.3 환자 정보 조회·수정 화면

```
GET /nurse/patient/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 특정 환자 기본 정보 조회 및 수정 폼 렌더링 |
| 인증 | ROLE_NURSE, ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `patientId` | Long | ✅ | 환자 ID |

**컨트롤러 반환**: `"nurse/patient/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `patient` | `PatientDto` | 환자 정보 |
| `successMessage` | `String` | Flash — 수정 완료 후 메시지 |
| `errorCode` | `String` | Flash — 오류 시 코드 |
| `errorMessage` | `String` | Flash — 오류 시 메시지 |

---

### 7.4 환자 정보 수정 처리

```
POST /nurse/patient/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 환자 기본 정보 수정 (이름, 연락처, 특이사항) |
| 인증 | ROLE_NURSE, ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `patientId` | Long | ✅ | 환자 ID |
| `name` | String | ✅ | 환자 성명 |
| `phone` | String | ✅ | 연락처 |
| `note` | String | 선택 | 특이사항 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/nurse/patient/detail?patientId={patientId}` |
| Flash | `successMessage` = `"환자 정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"nurse/patient/detail"` | `errorCode`, `errorMessage` |
| `VALIDATION_ERROR` | `"nurse/patient/detail"` | `errorCode`, `errorMessage`, `patient` |

---

### 7.5 NURSE 내 정보관리 화면

```
GET /nurse/mypage
```

| 항목 | 내용 |
|------|------|
| 설명 | 간호사 내 정보 관리 화면 렌더링 |
| 인증 | ROLE_NURSE |

**컨트롤러 반환**: `"nurse/mypage"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staff` | `StaffDto` | 로그인한 직원 정보 (이름, 사원번호, 부서 읽기전용 + 이메일, 연락처 수정가능) |
| `successMessage` | `String` | Flash — 수정 완료 메시지 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 7.6 NURSE 내 정보 수정 처리

```
POST /nurse/mypage/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 이메일·연락처 수정 또는 비밀번호 변경 |
| 인증 | ROLE_NURSE |

**Request Body (Form)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | 선택 | 이메일 |
| `phone` | String | 선택 | 연락처 |
| `currentPassword` | String | 선택 | 현재 비밀번호 (비밀번호 변경 시 필수) |
| `newPassword` | String | 선택 | 새 비밀번호 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/nurse/mypage` |
| Flash | `successMessage` = `"정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `INVALID_PASSWORD` | `"nurse/mypage"` | `errorCode`, `errorMessage`, `staff` |
| `VALIDATION_ERROR` | `"nurse/mypage"` | `errorCode`, `errorMessage`, `staff` |

---

## 8. LLM 규칙 챗봇 API

### 8.1 병원 규칙 질문 (AJAX)

```
POST /llm/rules/ask
```

| 항목 | 내용 |
|------|------|
| 설명 | 직원 질문을 Claude API에 전달하여 병원 규칙 기반 답변 반환 |
| 인증 | ROLE_DOCTOR, ROLE_NURSE |
| 처리 방식 | 서버 사이드 Claude API 호출 |
| 타임아웃 | 5초 초과 시 폴백 응답 반환 |
| 반환 | `@ResponseBody` JSON |

**Request Body**
```json
{
  "question": "응급 처치 키트는 어디에 보관되어 있나요?"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `question` | String | ✅ | 직원 질문 텍스트 (최대 500자) |

**응답 JSON (성공 — 규칙 내 답변)**
```json
{
  "success": true,
  "data": {
    "answer": "응급 키트는 3층 간호사 스테이션 좌측 캐비닛에 보관되어 있습니다."
  }
}
```

**응답 JSON (규칙 외 질문)**
```json
{
  "success": true,
  "data": {
    "answer": "등록된 병원 규칙에서 해당 질문의 답변을 확인할 수 없습니다. 관련 담당자에게 직접 문의해 주세요."
  }
}
```

**응답 JSON (LLM 실패 — 폴백)**
```json
{
  "success": false,
  "errorCode": "LLM_SERVICE_UNAVAILABLE",
  "message": "챗봇 서비스가 일시적으로 불가합니다. 관리자에게 문의해 주세요."
}
```

---

### 8.2 챗봇 대화 이력 조회 (AJAX)

```
GET /llm/rules/history
```

| 항목 | 내용 |
|------|------|
| 설명 | 현재 세션의 챗봇 대화 이력 조회 — 오버레이 열기 시 기존 이력 복원에 사용 |
| 인증 | ROLE_DOCTOR, ROLE_NURSE |
| 반환 | `@ResponseBody` JSON |

**응답 JSON (성공)**
```json
{
  "success": true,
  "data": {
    "history": [
      {
        "id": 1,
        "question": "응급 처치 키트 위치가 어디인가요?",
        "answer": "3층 간호사 스테이션 좌측 캐비닛에 보관되어 있습니다.",
        "createdAt": "2026-03-15T09:05:00"
      }
    ]
  }
}
```

---

## 9. 관리자 — 예약·환자 API (ROLE_ADMIN)

### 9.1 관리자 대시보드 화면

```
GET /admin/dashboard
```

| 항목 | 내용 |
|------|------|
| 설명 | 관리자 대시보드 — 단순 통계 5종 렌더링 |
| 인증 | ROLE_ADMIN |

**컨트롤러 반환**: `"admin/dashboard"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `todayReservationCount` | `Integer` | 오늘 예약 수 |
| `totalReservationCount` | `Integer` | 전체 예약 수 |
| `departmentStats` | `List<DeptStatDto>` | 진료과별 예약 수 |
| `itemShortageCount` | `Integer` | 재고 부족 물품 수 |
| `staffCount` | `Integer` | 전체 직원 수 |

---

### 9.2 대시보드 통계 조회 (AJAX)

```
GET /admin/dashboard/stats
```

| 항목 | 내용 |
|------|------|
| 설명 | 대시보드 통계 5종 데이터 반환 — 비동기 갱신용 |
| 인증 | ROLE_ADMIN |
| 반환 | `@ResponseBody` JSON |

**응답 JSON**
```json
{
  "success": true,
  "data": {
    "todayReservationCount": 18,
    "totalReservationCount": 1247,
    "departmentStats": [
      { "departmentName": "내과", "count": 432 },
      { "departmentName": "외과", "count": 389 }
    ],
    "itemShortageCount": 5,
    "staffCount": 42
  }
}
```

---

### 9.3 전체 접수 목록 화면

```
GET /admin/reception/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 당일 기준 전 직원·전 진료과 접수 현황 — 상태별 집계 + 예약 구분(온라인·전화·방문) 표시 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `date` | String | 선택 | 기준 날짜 (기본값: 오늘, yyyy-MM-dd) |
| `departmentId` | Long | 선택 | 진료과 필터 |
| `status` | String | 선택 | RESERVED / RECEIVED / COMPLETED / CANCELLED |
| `source` | String | 선택 | ONLINE / PHONE / WALKIN |
| `keyword` | String | 선택 | 환자명 검색 (부분 일치) |
| `page` | Integer | 선택 | 페이지 번호 (기본값: 0) |
| `size` | Integer | 선택 | 페이지 크기 (기본값: 20) |

**컨트롤러 반환**: `"admin/reception/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `date` | `String` | 조회 기준 날짜 |
| `summary` | `ReceptionSummaryDto` | 상태별 집계 |
| `receptions` | `List<ReceptionDto>` | 접수 목록 |
| `totalCount` | `Integer` | 전체 건수 |
| `page` | `Integer` | 현재 페이지 |
| `size` | `Integer` | 페이지 크기 |

---

### 9.4 전체 예약 목록 화면

```
GET /admin/reservation/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 전체 예약 조회 (날짜·상태·진료과 필터 가능) |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `startDate` | String | 선택 | 시작 날짜 (yyyy-MM-dd) |
| `endDate` | String | 선택 | 종료 날짜 (yyyy-MM-dd) |
| `status` | String | 선택 | 상태 필터 |
| `departmentId` | Long | 선택 | 진료과 필터 |
| `keyword` | String | 선택 | 환자명 검색 (부분 일치) |
| `page` | Integer | 선택 | 기본값: 0 |
| `size` | Integer | 선택 | 기본값: 20 |

**컨트롤러 반환**: `"admin/reservation/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `reservations` | `List<ReservationDto>` | 예약 목록 |
| `totalCount` | `Integer` | 전체 건수 |
| `page` | `Integer` | 현재 페이지 |
| `successMessage` | `String` | Flash — 취소 후 메시지 |
| `errorMessage` | `String` | Flash — 취소 실패 메시지 |

---

### 9.5 예약 취소 처리

```
POST /admin/reservation/cancel
```

| 항목 | 내용 |
|------|------|
| 설명 | 예약 취소 처리. COMPLETED 상태에서는 취소 불가 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `reservationId` | Long | ✅ | 예약 ID |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/reservation/list` |
| Flash | `successMessage` = `"예약이 취소되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `CANNOT_CANCEL_COMPLETED` | `"admin/reservation/list"` | `errorCode`, `errorMessage` |
| `ALREADY_CANCELLED` | `"admin/reservation/list"` | `errorCode`, `errorMessage` |
| `RESERVATION_NOT_FOUND` | `"admin/reservation/list"` | `errorCode`, `errorMessage` |

---

### 9.6 환자 목록 화면

```
GET /admin/patient/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 전체 환자 목록 조회 화면 렌더링 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `name` | String | 선택 | 환자 이름 검색 |
| `phone` | String | 선택 | 연락처 검색 |
| `page` | Integer | 선택 | 기본값: 0 |
| `size` | Integer | 선택 | 기본값: 20 |

**컨트롤러 반환**: `"admin/patient/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `patients` | `List<PatientDto>` | 환자 목록 |
| `totalCount` | `Integer` | 전체 건수 |
| `page` | `Integer` | 현재 페이지 |

---

### 9.7 환자 상세 조회 화면 (예약 이력 포함)

```
GET /admin/patient/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 특정 환자 상세 정보 + 전체 예약 이력 조회 화면 렌더링 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `patientId` | Long | ✅ | 환자 ID |

**컨트롤러 반환**: `"admin/patient/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `patient` | `PatientDto` | 환자 기본 정보 |
| `reservations` | `List<ReservationDto>` | 전체 예약 이력 |

---

## 10. 관리자 — 인사 관리 API (ROLE_ADMIN)

### 10.1 직원 목록 화면

```
GET /admin/staff/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 전체 직원 목록 화면 렌더링 (ROLE별 분류) |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `role` | String | 선택 | ADMIN / DOCTOR / NURSE / STAFF |
| `isActive` | Boolean | 선택 | 재직 여부 (기본값: true) |
| `keyword` | String | 선택 | 직원 이름 검색 (부분 일치) |

**컨트롤러 반환**: `"admin/staff/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staffList` | `List<StaffDto>` | 직원 목록 |
| `totalCount` | `Integer` | 전체 건수 |
| `successMessage` | `String` | Flash — 비활성화 완료 후 메시지 |

---

### 10.2 직원 등록 화면

```
GET /admin/staff/new
```

| 항목 | 내용 |
|------|------|
| 설명 | 직원 등록 폼 렌더링 (진료과 목록 포함) |
| 인증 | ROLE_ADMIN |

**컨트롤러 반환**: `"admin/staff/new"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `departments` | `List<DepartmentDto>` | 활성 진료과 목록 |
| `errorCode` | `String` | Flash — 등록 실패 코드 |
| `errorMessage` | `String` | Flash — 등록 실패 메시지 |
| `inputData` | `StaffFormDto` | Flash — 입력값 복원용 |

---

### 10.3 직원 등록 처리

```
POST /admin/staff/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 직원 등록. ROLE_DOCTOR인 경우 Doctor 레코드 동시 저장 (단일 트랜잭션) |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `username` | String | ✅ | 로그인 ID (UNIQUE) |
| `password` | String | ✅ | 비밀번호 (BCrypt 암호화) |
| `name` | String | ✅ | 직원 실명 |
| `role` | String | ✅ | ADMIN / DOCTOR / NURSE / STAFF |
| `departmentId` | Long | 선택 | 소속 진료과 ID |
| `availableDays` | Array | 선택 | 진료 가능 요일 (DOCTOR만) |
| `specialty` | String | 선택 | 전문 분야 (DOCTOR만) |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/staff/list` |
| Flash | `successMessage` = `"직원이 등록되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `DUPLICATE_USERNAME` | `"admin/staff/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |
| `VALIDATION_ERROR` | `"admin/staff/new"` | `errorCode`, `errorMessage`, `inputData`, `departments` |

---

### 10.4 직원 상세·수정 화면

```
GET /admin/staff/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 특정 직원 상세 정보 조회 및 수정 폼 렌더링 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `staffId` | Long | ✅ | 직원 ID |

**컨트롤러 반환**: `"admin/staff/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staff` | `StaffDto` | 직원 상세 정보 |
| `departments` | `List<DepartmentDto>` | 활성 진료과 목록 |
| `successMessage` | `String` | Flash — 수정 완료 메시지 |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 10.5 직원 정보 수정 처리

```
POST /admin/staff/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 직원 정보 수정 (ROLE_DOCTOR인 경우 Doctor 정보도 동시 수정) |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `staffId` | Long | ✅ | 직원 ID |
| `name` | String | ✅ | 직원 실명 |
| `departmentId` | Long | 선택 | 소속 진료과 ID |
| `availableDays` | Array | 선택 | 진료 가능 요일 (DOCTOR만) |
| `specialty` | String | 선택 | 전문 분야 (DOCTOR만) |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/staff/detail?staffId={staffId}` |
| Flash | `successMessage` = `"직원 정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/staff/list"` | `errorCode`, `errorMessage` |
| `VALIDATION_ERROR` | `"admin/staff/detail"` | `errorCode`, `errorMessage`, `staff`, `departments` |

---

### 10.6 직원 비활성화 처리

```
POST /admin/staff/deactivate
```

| 항목 | 내용 |
|------|------|
| 설명 | 직원 재직 상태를 비활성화 (is_active = FALSE). 삭제 아님. |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `staffId` | Long | ✅ | 직원 ID |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/staff/list` |
| Flash | `successMessage` = `"직원이 비활성화되었습니다. 해당 직원은 더 이상 로그인할 수 없습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/staff/list"` | `errorCode`, `errorMessage` |

---

## 11. 관리자 — 진료과 API (ROLE_ADMIN)

### 11.1 진료과 목록 화면

```
GET /admin/department/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 전체 진료과 목록 화면 렌더링 |
| 인증 | ROLE_ADMIN |

**컨트롤러 반환**: `"admin/department/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `departments` | `List<DepartmentDto>` | 전체 진료과 목록 |
| `successMessage` | `String` | Flash — 처리 완료 메시지 |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 11.2 진료과 등록 처리

```
POST /admin/department/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 새 진료과 등록 (인라인 폼 처리) |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | ✅ | 진료과명 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/department/list` |
| Flash | `successMessage` = `"진료과가 등록되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `VALIDATION_ERROR` | `"admin/department/list"` | `errorCode`, `errorMessage`, `departments` |

---

### 11.3 진료과 상세·수정 화면

```
GET /admin/department/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 진료과 상세 화면 렌더링 — 진료과 정보 + 소속 의사 목록 + 예약 통계 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `departmentId` | Long | ✅ | 진료과 ID |

**컨트롤러 반환**: `"admin/department/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `department` | `DepartmentDto` | 진료과 정보 (id, name, isActive) |
| `doctors` | `List<DoctorDto>` | 소속 의사 목록 |
| `stats` | `DeptStatsDto` | 예약 통계 (전체/이번달/이번주) |
| `successMessage` | `String` | Flash — 수정/활성화/비활성화 후 메시지 |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 11.4 진료과 정보 수정 처리

```
POST /admin/department/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 진료과 정보 수정 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `departmentId` | Long | ✅ | 진료과 ID |
| `name` | String | ✅ | 진료과명 |
| `isActive` | Boolean | ✅ | 운영 여부 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/department/detail?departmentId={departmentId}` |
| Flash | `successMessage` = `"진료과 정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/department/list"` | `errorCode`, `errorMessage` |
| `VALIDATION_ERROR` | `"admin/department/detail"` | `errorCode`, `errorMessage`, `department`, `doctors`, `stats` |

---

### 11.5 진료과 비활성화 처리

```
POST /admin/department/deactivate
```

| 항목 | 내용 |
|------|------|
| 설명 | is_active = FALSE. 비활성 진료과는 예약 화면 및 LLM 프롬프트에서 제외. 화면 25(진료과 목록)에서 비활성화 버튼 클릭 시 호출 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `departmentId` | Long | ✅ | 진료과 ID |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/department/list` |
| Flash | `successMessage` = `"진료과가 비활성화되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/department/list"` | `errorCode`, `errorMessage` |

---

### 11.6 진료과 활성화 처리

```
POST /admin/department/activate
```

| 항목 | 내용 |
|------|------|
| 설명 | 비활성 진료과를 활성화 (is_active = TRUE) — 예약 화면 및 LLM 프롬프트에 즉시 반영. 화면 25(진료과 목록)에서 활성화 버튼 클릭 시 호출 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `departmentId` | Long | ✅ | 진료과 ID |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/department/list` |
| Flash | `successMessage` = `"진료과가 활성화되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/department/list"` | `errorCode`, `errorMessage` |

---

## 12. 관리자 — 물품 관리 API (ROLE_ADMIN)

### 12.1 물품 목록 화면

```
GET /admin/item/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 전체 물품 목록 화면 렌더링 (재고 부족 항목 강조 포함) |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `category` | String | 선택 | 카테고리 필터 |
| `keyword` | String | 선택 | 물품명 검색 (부분 일치) |
| `stockStatus` | String | 선택 | 재고 상태 (shortage/normal/all, 기본값: all) |

**컨트롤러 반환**: `"admin/item/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `items` | `List<ItemDto>` | 물품 목록 |
| `totalCount` | `Integer` | 전체 건수 |
| `shortageCount` | `Integer` | 재고 부족 건수 |
| `successMessage` | `String` | Flash — 처리 완료 메시지 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 12.2 물품 등록 화면

```
GET /admin/item/new
```

| 항목 | 내용 |
|------|------|
| 설명 | 물품 등록 폼 렌더링 |
| 인증 | ROLE_ADMIN |

**컨트롤러 반환**: `"admin/item/new"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `categories` | `List<String>` | 카테고리 옵션 (MEDICAL_SUPPLIES / MEDICAL_EQUIPMENT / GENERAL_SUPPLIES) |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |
| `inputData` | `ItemFormDto` | Flash — 입력값 복원용 |

---

### 12.3 물품 등록 처리

```
POST /admin/item/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 새 물품 등록 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | ✅ | 물품명 |
| `category` | String | ✅ | MEDICAL_SUPPLIES / MEDICAL_EQUIPMENT / GENERAL_SUPPLIES |
| `quantity` | Integer | ✅ | 현재 수량 |
| `minQuantity` | Integer | ✅ | 최소 재고 기준 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/item/list` |
| Flash | `successMessage` = `"물품이 등록되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `VALIDATION_ERROR` | `"admin/item/new"` | `errorCode`, `errorMessage`, `inputData`, `categories` |

---

### 12.4 물품 상세·수정 화면

```
GET /admin/item/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 물품 상세 정보 조회 및 수정 폼 렌더링 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `itemId` | Long | ✅ | 물품 ID |

**컨트롤러 반환**: `"admin/item/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `item` | `ItemDto` | 물품 상세 정보 |
| `categories` | `List<String>` | 카테고리 옵션 목록 |
| `successMessage` | `String` | Flash — 수정 완료 메시지 |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 12.5 물품 전체 정보 수정 처리

```
POST /admin/item/update
```

> v2.0의 `POST /admin/item/updateQuantity` (수량만 수정)를 대체합니다.

| 항목 | 내용 |
|------|------|
| 설명 | 물품 이름·카테고리·현재 수량·최소 수량 전체 수정 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `itemId` | Long | ✅ | 물품 ID |
| `name` | String | ✅ | 물품명 (최대 200자) |
| `category` | String | ✅ | MEDICAL_SUPPLIES / MEDICAL_EQUIPMENT / GENERAL_SUPPLIES |
| `quantity` | Integer | ✅ | 현재 수량 (0 이상) |
| `minQuantity` | Integer | ✅ | 최소 수량 (0 이상) |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/item/detail?itemId={itemId}` |
| Flash | `successMessage` = `"물품 정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/item/list"` | `errorCode`, `errorMessage` |
| `VALIDATION_ERROR` | `"admin/item/detail"` | `errorCode`, `errorMessage`, `item`, `categories` |

---

### 12.6 물품 삭제 처리

```
POST /admin/item/delete
```

| 항목 | 내용 |
|------|------|
| 설명 | 물품 삭제 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `itemId` | Long | ✅ | 물품 ID |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/item/list` |
| Flash | `successMessage` = `"물품이 삭제되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/item/list"` | `errorCode`, `errorMessage` |

---

## 13. 관리자 — 병원 규칙 API (ROLE_ADMIN)

### 13.1 규칙 목록 화면

```
GET /admin/rule/list
```

| 항목 | 내용 |
|------|------|
| 설명 | 전체 병원 규칙 목록 화면 렌더링 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `category` | String | 선택 | 카테고리 필터 |
| `isActive` | Boolean | 선택 | 활성 여부 필터 |
| `keyword` | String | 선택 | 규칙 제목 검색 (부분 일치) |

**컨트롤러 반환**: `"admin/rule/list"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `rules` | `List<RuleDto>` | 병원 규칙 목록 |
| `totalCount` | `Integer` | 전체 건수 |
| `activeCount` | `Integer` | 활성 건수 |
| `successMessage` | `String` | Flash — 처리 완료 메시지 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 13.2 규칙 등록 화면

```
GET /admin/rule/new
```

| 항목 | 내용 |
|------|------|
| 설명 | 병원 규칙 등록 폼 렌더링 |
| 인증 | ROLE_ADMIN |

**컨트롤러 반환**: `"admin/rule/new"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `categories` | `List<String>` | 카테고리 옵션 (EMERGENCY / SUPPLY / DUTY / HYGIENE / OTHER) |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |
| `inputData` | `RuleFormDto` | Flash — 입력값 복원용 |

---

### 13.3 규칙 등록 처리

```
POST /admin/rule/create
```

| 항목 | 내용 |
|------|------|
| 설명 | 병원 규칙 등록. 즉시 챗봇 프롬프트에 반영됨 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | String | ✅ | 규칙 제목 (최대 200자) |
| `content` | String | ✅ | 규칙 본문 텍스트 |
| `category` | String | ✅ | EMERGENCY / SUPPLY / DUTY / HYGIENE / OTHER |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/rule/list` |
| Flash | `successMessage` = `"규칙이 등록되었습니다. 챗봇에 즉시 반영됩니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `VALIDATION_ERROR` | `"admin/rule/new"` | `errorCode`, `errorMessage`, `inputData`, `categories` |

---

### 13.4 규칙 상세·수정 화면

```
GET /admin/rule/detail
```

| 항목 | 내용 |
|------|------|
| 설명 | 규칙 상세 정보 조회 및 수정 폼 렌더링 |
| 인증 | ROLE_ADMIN |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `ruleId` | Long | ✅ | 규칙 ID |

**컨트롤러 반환**: `"admin/rule/detail"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `rule` | `RuleDto` | 규칙 정보 |
| `categories` | `List<String>` | 카테고리 옵션 목록 |
| `successMessage` | `String` | Flash — 수정/토글 완료 메시지 |
| `errorCode` | `String` | Flash — 오류 코드 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 13.5 규칙 수정 처리

```
POST /admin/rule/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 병원 규칙 수정. 즉시 챗봇에 반영됨 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `ruleId` | Long | ✅ | 규칙 ID |
| `title` | String | ✅ | 규칙 제목 |
| `content` | String | ✅ | 규칙 본문 |
| `category` | String | ✅ | 카테고리 코드 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/rule/detail?ruleId={ruleId}` |
| Flash | `successMessage` = `"규칙이 수정되었습니다. 챗봇에 즉시 반영됩니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/rule/list"` | `errorCode`, `errorMessage` |
| `VALIDATION_ERROR` | `"admin/rule/detail"` | `errorCode`, `errorMessage`, `rule`, `categories` |

---

### 13.6 규칙 활성화 상태 토글

```
POST /admin/rule/toggleActive
```

| 항목 | 내용 |
|------|------|
| 설명 | is_active 토글. FALSE 시 챗봇 프롬프트에서 즉시 제외 |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `ruleId` | Long | ✅ | 규칙 ID |

**성공 처리 (활성화 → 비활성화)**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/rule/list` |
| Flash | `successMessage` = `"규칙이 비활성화되었습니다. 챗봇에서 제외됩니다."` |

**성공 처리 (비활성화 → 활성화)**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/rule/list` |
| Flash | `successMessage` = `"규칙이 활성화되었습니다. 챗봇에 즉시 반영됩니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/rule/list"` | `errorCode`, `errorMessage` |

---

### 13.7 규칙 삭제 처리

```
POST /admin/rule/delete
```

| 항목 | 내용 |
|------|------|
| 설명 | 병원 규칙 삭제. 삭제 전 비활성화 권장. |
| 인증 | ROLE_ADMIN |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `ruleId` | Long | ✅ | 규칙 ID |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/rule/list` |
| Flash | `successMessage` = `"규칙이 삭제되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `RESOURCE_NOT_FOUND` | `"admin/rule/list"` | `errorCode`, `errorMessage` |

---

## 14. 관리자 — 대시보드 API (ROLE_ADMIN)

> `GET /admin/dashboard` 및 `GET /admin/dashboard/stats` 는 [9.1](#91-관리자-대시보드-화면), [9.2](#92-대시보드-통계-조회-ajax) 를 참조하세요.

### 14.3 관리자 내 정보관리 화면

```
GET /admin/mypage
```

| 항목 | 내용 |
|------|------|
| 설명 | 관리자 내 정보 관리 화면 렌더링 |
| 인증 | ROLE_ADMIN |

**컨트롤러 반환**: `"admin/mypage"`

**Request Attributes**

| 키 | 타입 | 설명 |
|----|------|------|
| `staff` | `StaffDto` | 로그인한 직원 정보 (이름, 사원번호 읽기전용 + 이메일, 연락처 수정가능) |
| `successMessage` | `String` | Flash — 수정 완료 메시지 |
| `errorMessage` | `String` | Flash — 오류 메시지 |

---

### 14.4 관리자 내 정보 수정 처리

```
POST /admin/mypage/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 이메일·연락처 수정 또는 비밀번호 변경 |
| 인증 | ROLE_ADMIN |

**Request Body (Form)**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | 선택 | 이메일 |
| `phone` | String | 선택 | 연락처 |
| `currentPassword` | String | 선택 | 현재 비밀번호 (비밀번호 변경 시 필수) |
| `newPassword` | String | 선택 | 새 비밀번호 |

**성공 처리**

| 항목 | 내용 |
|------|------|
| 반환 | `redirect:/admin/mypage` |
| Flash | `successMessage` = `"정보가 수정되었습니다."` |

**오류 처리**

| 오류 코드 | 반환 뷰 | Attribute |
|-----------|---------|-----------|
| `INVALID_PASSWORD` | `"admin/mypage"` | `errorCode`, `errorMessage`, `staff` |
| `VALIDATION_ERROR` | `"admin/mypage"` | `errorCode`, `errorMessage`, `staff` |

---

## 15. JSON API 레이어 (/api/**)

> PBL 최소 요구사항 충족을 위한 JSON API 엔드포인트. 모든 응답은 JSON `@ResponseBody`이며, Path Variable로 ID를 전달한다.

### 15.1 직원 정보 수정

```
POST /api/staff/{id}/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 직원 정보 수정 (JSON API) |
| 인증 | ROLE_ADMIN |
| 반환 | `@ResponseBody` JSON |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 직원 ID |

**Request Body (JSON)**

```json
{
  "name": "김철수",
  "departmentId": 2,
  "specialty": "소화기내과",
  "availableDays": ["MON", "WED", "FRI"]
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | ✅ | 직원 실명 |
| `departmentId` | Long | 선택 | 소속 진료과 ID |
| `specialty` | String | 선택 | 전문 분야 (DOCTOR만) |
| `availableDays` | Array | 선택 | 진료 가능 요일 (DOCTOR만) |

**응답 JSON (200 OK)**

```json
{
  "success": true,
  "data": {
    "staffId": 5,
    "name": "김철수",
    "departmentId": 2,
    "departmentName": "내과",
    "specialty": "소화기내과",
    "availableDays": ["MON", "WED", "FRI"]
  },
  "message": "직원 정보가 수정되었습니다."
}
```

**오류 응답**

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `404` | `RESOURCE_NOT_FOUND` | 직원 ID 없음 |
| `400` | `VALIDATION_ERROR` | 필수 필드 누락 / 형식 오류 |

---

### 15.2 환자 정보 부분 수정

```
POST /api/patients/{id}/update
```

| 항목 | 내용 |
|------|------|
| 설명 | 환자 정보 수정 (JSON API) |
| 인증 | ROLE_NURSE, ROLE_ADMIN |
| 반환 | `@ResponseBody` JSON |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 환자 ID |

**Request Body (JSON)** — 수정할 필드만 포함

```json
{
  "name": "홍길동",
  "phone": "010-1234-5678",
  "note": "약물 알레르기 있음"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | 선택 | 환자 성명 |
| `phone` | String | 선택 | 연락처 |
| `note` | String | 선택 | 특이사항 |

**응답 JSON (200 OK)**

```json
{
  "success": true,
  "data": {
    "patientId": 10,
    "name": "홍길동",
    "phone": "010-1234-5678",
    "note": "약물 알레르기 있음"
  },
  "message": "환자 정보가 수정되었습니다."
}
```

**오류 응답**

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `404` | `RESOURCE_NOT_FOUND` | 환자 ID 없음 |
| `400` | `VALIDATION_ERROR` | 형식 오류 |

---

### 15.3 예약 취소

```
POST /api/reservations/{id}/cancel
```

| 항목 | 내용 |
|------|------|
| 설명 | 예약 취소 처리 (JSON API). COMPLETED 상태에서는 취소 불가 |
| 인증 | ROLE_ADMIN |
| 반환 | `@ResponseBody` JSON |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 예약 ID |

**응답 JSON (200 OK)**

```json
{
  "success": true,
  "message": "예약이 취소되었습니다."
}
```

**오류 응답**

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `404` | `RESOURCE_NOT_FOUND` | 예약 ID 없음 |
| `409` | `CANNOT_CANCEL_COMPLETED` | 진료 완료 예약 취소 불가 |
| `409` | `ALREADY_CANCELLED` | 이미 취소된 예약 |

---

### 15.4 물품 삭제

```
POST /api/items/{id}/delete
```

| 항목 | 내용 |
|------|------|
| 설명 | 물품 삭제 (JSON API) |
| 인증 | ROLE_ADMIN |
| 반환 | `@ResponseBody` JSON |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 물품 ID |

**응답 JSON (200 OK)**

```json
{
  "success": true,
  "message": "물품이 삭제되었습니다."
}
```

**오류 응답**

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `404` | `RESOURCE_NOT_FOUND` | 물품 ID 없음 |

---

### 15.5 규칙 삭제

```
POST /api/rules/{id}/delete
```

| 항목 | 내용 |
|------|------|
| 설명 | 병원 규칙 삭제 (JSON API) |
| 인증 | ROLE_ADMIN |
| 반환 | `@ResponseBody` JSON |

**Path Variable**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | Long | 규칙 ID |

**응답 JSON (200 OK)**

```json
{
  "success": true,
  "message": "규칙이 삭제되었습니다."
}
```

**오류 응답**

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|-----------|------|
| `404` | `RESOURCE_NOT_FOUND` | 규칙 ID 없음 |

---

## 16. 에러 코드 정의

### 16.1 예약 관련

| 에러 코드 | HTTP 상태 | 설명 | SSR 처리 |
|-----------|-----------|------|----------|
| `DUPLICATE_RESERVATION` | `409` | 동일 의사·날짜·시간 중복 예약 | 폼 재렌더링 + `errorCode` / `errorMessage` |
| `INVALID_TIME_SLOT` | `400` | 유효하지 않은 시간 슬롯 | 폼 재렌더링 |
| `DOCTOR_NOT_AVAILABLE` | `400` | 해당 날짜 의사 진료 불가 | 폼 재렌더링 |
| `RESERVATION_NOT_FOUND` | `404` | 예약 ID 없음 | 목록 화면 + `errorMessage` Flash |
| `CANNOT_CANCEL_COMPLETED` | `409` | 진료 완료 예약 취소 불가 | 목록 화면 + `errorMessage` Flash |

### 16.2 상태 전이 관련

| 에러 코드 | HTTP 상태 | 설명 | SSR 처리 |
|-----------|-----------|------|----------|
| `INVALID_STATUS_TRANSITION` | `409` | 허용되지 않는 상태 전이 | 상세 화면 재렌더링 |
| `ALREADY_CANCELLED` | `409` | 이미 취소된 예약 | 목록 화면 + Flash |
| `ALREADY_COMPLETED` | `409` | 이미 완료된 예약 | 목록 화면 + Flash |

### 16.3 인증·권한 관련

| 에러 코드 | HTTP 상태 | 설명 | SSR 처리 |
|-----------|-----------|------|----------|
| `UNAUTHORIZED` | `401` | 미인증 접근 | Spring Security → `/login` |
| `ACCESS_DENIED` | `403` | 권한 없는 접근 | Spring Security → 403 오류 화면 |
| `NOT_OWN_PATIENT` | `403` | 본인 담당이 아닌 환자 접근 | 목록 화면 + `errorMessage` Flash |
| `INVALID_PASSWORD` | `400` | 현재 비밀번호 불일치 | 마이페이지 폼 재렌더링 |

### 16.4 LLM 관련 (AJAX — JSON 응답 유지)

| 에러 코드 | HTTP 상태 | 설명 |
|-----------|-----------|------|
| `LLM_SERVICE_UNAVAILABLE` | `503` | Claude API 호출 실패 / 타임아웃 |
| `LLM_PARSE_ERROR` | `500` | LLM 응답 JSON 파싱 오류 |

### 16.5 데이터 관련

| 에러 코드 | HTTP 상태 | 설명 | SSR 처리 |
|-----------|-----------|------|----------|
| `RESOURCE_NOT_FOUND` | `404` | 요청한 리소스 없음 | 목록 화면 + `errorMessage` Flash |
| `DUPLICATE_USERNAME` | `409` | 이미 존재하는 로그인 ID | 등록 폼 재렌더링 |
| `VALIDATION_ERROR` | `400` | 필수 필드 누락 / 형식 오류 | 폼 재렌더링 + 입력값 복원 |

---

## 17. 전체 API 목록 요약

**총 75개 엔드포인트 | GET 35개 · POST 40개**

### 비회원 / 공통 (인증 불필요)

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 1 | GET | `/` | `"index"` | 비회원 메인 화면 |
| 2 | GET | `/login` | `"auth/login"` | 로그인 화면 |
| 3 | POST | `/login` | redirect (Security) | 로그인 처리 |
| 4 | POST | `/logout` | redirect (Security) | 로그아웃 처리 |
| 5 | GET | `/reservation` | `"reservation/index"` | 예약 방식 선택 화면 |
| 6 | GET | `/reservation/symptom` | `"reservation/symptom"` | 증상 입력 화면 |
| 7 | POST | `/llm/symptom/analyze` | JSON (AJAX) | LLM 증상 분석·추천 |
| 8 | GET | `/reservation/getDoctors` | JSON (AJAX) | 진료과별 의사 목록 |
| 9 | GET | `/reservation/getSlots` | JSON (AJAX) | 예약 가능 시간 슬롯 |
| 10 | GET | `/reservation/direct` | `"reservation/direct"` | 직접 선택 예약 화면 |
| 11 | POST | `/reservation/create` | redirect or 폼재렌더링 | 예약 생성 |
| 12 | GET | `/reservation/complete` | `"reservation/complete"` | 예약 완료 화면 |

### ROLE_STAFF (접수 직원)

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 13 | GET | `/staff/dashboard` | `"staff/dashboard"` | STAFF 대시보드 |
| 14 | GET | `/staff/reception/list` | `"staff/reception/list"` | 접수 목록 화면 |
| 15 | GET | `/staff/reception/detail` | `"staff/reception/detail"` | 접수 처리 화면 |
| 16 | POST | `/staff/reception/receive` | redirect or 폼재렌더링 | 접수 완료 처리 |
| 17 | GET | `/staff/reservation/new` | `"staff/reservation/new"` | 전화 예약 등록 화면 |
| 18 | POST | `/staff/reservation/create` | redirect or 폼재렌더링 | 전화 예약 등록 처리 |
| 19 | GET | `/staff/walkin/new` | `"staff/walkin/new"` | 방문 접수 화면 |
| 20 | POST | `/staff/walkin/create` | redirect or 폼재렌더링 | 방문 접수 등록 처리 |
| 21 | GET | `/staff/mypage` | `"staff/mypage"` | STAFF 내 정보관리 화면 |
| 22 | POST | `/staff/mypage/update` | redirect | STAFF 내 정보 수정 처리 |

### ROLE_DOCTOR (의사)

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 23 | GET | `/doctor/dashboard` | `"doctor/dashboard"` | DOCTOR 대시보드 |
| 24 | GET | `/doctor/treatment/list` | `"doctor/treatment/list"` | 오늘 진료 목록 화면 |
| 25 | GET | `/doctor/treatment/detail` | `"doctor/treatment/detail"` | 진료 기록 입력 화면 |
| 26 | POST | `/doctor/treatment/complete` | redirect or 폼재렌더링 | 진료 완료 처리 |
| 27 | GET | `/doctor/mypage` | `"doctor/mypage"` | DOCTOR 내 정보관리 화면 |
| 28 | POST | `/doctor/mypage/update` | redirect | DOCTOR 내 정보 수정 처리 |

### ROLE_NURSE (간호사)

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 29 | GET | `/nurse/dashboard` | `"nurse/dashboard"` | NURSE 대시보드 |
| 30 | GET | `/nurse/schedule/list` | `"nurse/schedule/list"` | 오늘 예약 현황 화면 |
| 31 | GET | `/nurse/patient/detail` | `"nurse/patient/detail"` | 환자 정보 조회·수정 화면 |
| 32 | POST | `/nurse/patient/update` | redirect or 폼재렌더링 | 환자 정보 수정 처리 |
| 33 | GET | `/nurse/mypage` | `"nurse/mypage"` | NURSE 내 정보관리 화면 |
| 34 | POST | `/nurse/mypage/update` | redirect | NURSE 내 정보 수정 처리 |

### ROLE_DOCTOR + ROLE_NURSE (LLM 챗봇)

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 35 | POST | `/llm/rules/ask` | JSON (AJAX) | 규칙 Q&A 챗봇 질의 |
| 36 | GET | `/llm/rules/history` | JSON (AJAX) | 현재 세션 챗봇 이력 조회 |

### ROLE_ADMIN — 대시보드·예약·환자

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 37 | GET | `/admin/dashboard` | `"admin/dashboard"` | 관리자 대시보드 화면 |
| 38 | GET | `/admin/dashboard/stats` | JSON (AJAX) | 대시보드 통계 데이터 |
| 39 | GET | `/admin/reception/list` | `"admin/reception/list"` | 전체 접수 목록 화면 |
| 40 | GET | `/admin/mypage` | `"admin/mypage"` | 관리자 내 정보관리 화면 |
| 41 | POST | `/admin/mypage/update` | redirect | 관리자 내 정보 수정 처리 |
| 42 | GET | `/admin/reservation/list` | `"admin/reservation/list"` | 전체 예약 목록 화면 |
| 43 | POST | `/admin/reservation/cancel` | redirect or Flash | 예약 취소 처리 |
| 44 | GET | `/admin/patient/list` | `"admin/patient/list"` | 환자 목록 화면 |
| 45 | GET | `/admin/patient/detail` | `"admin/patient/detail"` | 환자 상세·이력 화면 |

### ROLE_ADMIN — 직원 관리

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 46 | GET | `/admin/staff/list` | `"admin/staff/list"` | 직원 목록 화면 |
| 47 | GET | `/admin/staff/new` | `"admin/staff/new"` | 직원 등록 화면 |
| 48 | POST | `/admin/staff/create` | redirect or 폼재렌더링 | 직원 등록 처리 |
| 49 | GET | `/admin/staff/detail` | `"admin/staff/detail"` | 직원 상세·수정 화면 |
| 50 | POST | `/admin/staff/update` | redirect or 폼재렌더링 | 직원 정보 수정 처리 |
| 51 | POST | `/admin/staff/deactivate` | redirect | 직원 비활성화 처리 |

### ROLE_ADMIN — 진료과 관리

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 52 | GET | `/admin/department/list` | `"admin/department/list"` | 진료과 목록 화면 |
| 53 | POST | `/admin/department/create` | redirect or 폼재렌더링 | 진료과 등록 처리 |
| 54 | GET | `/admin/department/detail` | `"admin/department/detail"` | 진료과 상세·수정 화면 |
| 55 | POST | `/admin/department/update` | redirect or 폼재렌더링 | 진료과 수정 처리 |
| 56 | POST | `/admin/department/deactivate` | redirect | 진료과 비활성화 처리 |
| 57 | POST | `/admin/department/activate` | redirect | 진료과 활성화 처리 |

### ROLE_ADMIN — 물품 관리

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 58 | GET | `/admin/item/list` | `"admin/item/list"` | 물품 목록 화면 |
| 59 | GET | `/admin/item/new` | `"admin/item/new"` | 물품 등록 화면 |
| 60 | POST | `/admin/item/create` | redirect or 폼재렌더링 | 물품 등록 처리 |
| 61 | GET | `/admin/item/detail` | `"admin/item/detail"` | 물품 상세·수정 화면 |
| 62 | POST | `/admin/item/update` | redirect or 폼재렌더링 | 물품 전체 수정 처리 |
| 63 | POST | `/admin/item/delete` | redirect | 물품 삭제 처리 |

### ROLE_ADMIN — 병원 규칙 관리

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 64 | GET | `/admin/rule/list` | `"admin/rule/list"` | 병원 규칙 목록 화면 |
| 65 | GET | `/admin/rule/new` | `"admin/rule/new"` | 병원 규칙 등록 화면 |
| 66 | POST | `/admin/rule/create` | redirect or 폼재렌더링 | 규칙 등록 처리 |
| 67 | GET | `/admin/rule/detail` | `"admin/rule/detail"` | 병원 규칙 상세·수정 화면 |
| 68 | POST | `/admin/rule/update` | redirect or 폼재렌더링 | 규칙 수정 처리 |
| 69 | POST | `/admin/rule/toggleActive` | redirect | 규칙 활성화 토글 |
| 70 | POST | `/admin/rule/delete` | redirect | 규칙 삭제 처리 |

### JSON API 레이어 (/api/**)

| # | 메서드 | URL | 반환 | 설명 |
|---|--------|-----|------|------|
| 71 | POST | `/api/staff/{id}/update` | JSON | 직원 정보 수정 |
| 72 | POST | `/api/patients/{id}/update` | JSON | 환자 정보 수정 |
| 73 | POST | `/api/reservations/{id}/cancel` | JSON | 예약 취소 |
| 74 | POST | `/api/items/{id}/delete` | JSON | 물품 삭제 |
| 75 | POST | `/api/rules/{id}/delete` | JSON | 규칙 삭제 |

---

## URL 설계 원칙 요약

| 액션 | 메서드 | URL 패턴 | 컨트롤러 반환 |
|------|--------|----------|--------------|
| 목록 화면 | GET | `/{역할}/{자원}/list` | 뷰 경로 |
| 상세 화면 | GET | `/{역할}/{자원}/detail?{자원}Id={id}` | 뷰 경로 |
| 등록 화면 | GET | `/{역할}/{자원}/new` | 뷰 경로 |
| 생성 처리 | POST | `/{역할}/{자원}/create` | redirect (성공) / 뷰 경로 (실패) |
| 수정 처리 | POST | `/{역할}/{자원}/update` | redirect (성공) / 뷰 경로 (실패) |
| 삭제 처리 | POST | `/{역할}/{자원}/delete` | redirect |
| 비활성화 | POST | `/{역할}/{자원}/deactivate` | redirect |
| 활성화 | POST | `/{역할}/{자원}/activate` | redirect |
| 상태 변경 | POST | `/{역할}/{자원}/{액션}` | redirect (성공) / 뷰 경로 (실패) |
| 내 정보관리 | GET | `/{역할}/mypage` | SSR |
| 내 정보 수정 | POST | `/{역할}/mypage/update` | SSR (PRG) |

### JSON API URL 설계 원칙 (/api/**)

| 액션 | 메서드 | URL 패턴 | 컨트롤러 반환 |
|------|--------|----------|--------------|
| 수정 | POST | `/api/{자원}/{id}/update` | JSON |
| 취소 | POST | `/api/{자원}/{id}/cancel` | JSON |
| 삭제 | POST | `/api/{자원}/{id}/delete` | JSON |

---

*본 API 명세서는 프로젝트 계획서 v4.2, ERD v2.0, 화면 정의서 v2.0을 기반으로 작성되었습니다.*
*변경 발생 시 GitHub Wiki에서 버전 이력을 관리합니다.*</content>
</invoke>