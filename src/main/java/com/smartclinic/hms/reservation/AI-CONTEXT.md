<!-- Parent: ../AI-CONTEXT.md -->

# reservation — 비회원 예약

## 목적

환자(비회원)가 온라인으로 예약하는 전체 흐름 (S00~S04).
AI 증상 분석 연동 및 직접 선택 예약 모두 지원. **개발자 A(강태오) 소유.**

## 하위 패키지

`reservation/reservation/` (실제 소스 위치)

## 주요 파일

| 파일 | 설명 |
|------|------|
| ReservationController.java | SSR GET 매핑 (`@Controller`, `@RequestMapping("/reservation")`) |
| ReservationService.java | 예약 생성 비즈니스 로직, 의사 목록 조회 |
| ReservationApiController.java | AJAX REST (`@RestController`, `@RequestMapping("/api/reservation")`) |
| ReservationRepository.java | Reservation JPA Repository |
| PatientRepository.java | Patient findByPhone (전화번호로 findOrCreate) |
| DepartmentRepository.java | Department 목록 조회 |
| ReservationCreateForm.java | 예약 생성 폼 DTO (Record, @Valid) |
| ReservationCompleteInfo.java | 예약 완료 화면용 DTO |

## URL 라우팅

| 메서드 | URL | 화면 |
|--------|-----|------|
| GET | `/reservation` | S00 예약방법 선택 |
| GET | `/reservation/symptom-reservation` | S01 증상 입력 + AI 분석 |
| GET | `/reservation/direct-reservation` | S03 직접 예약 폼 |
| GET | `/reservation/complete` | S04 예약 완료 |
| POST | `/reservation/create` | 예약 저장 (PRG → /complete) |
| GET | `/api/reservation/doctors?departmentId=` | 진료과별 의사 목록 (AJAX) |
| GET | `/api/reservation/slots?doctorId=&date=` | 가용 시간 슬롯 (AJAX) |

## 핵심 구현 패턴

```java
// GET
@GetMapping("")
public String patientChoice(HttpServletRequest request) {
    request.setAttribute("pageTitle", "진료 예약");
    return "reservation/patient-choice";
}

// POST PRG
@PostMapping("/create")
public String create(@Valid ReservationCreateForm form, BindingResult result, ...) {
    if (result.hasErrors()) return "reservation/direct-reservation"; // 폼 재렌더링
    String num = reservationService.createReservation(form);
    return "redirect:/reservation/complete?reservationNumber=" + num;
}
```

## AI 작업 지침

- `DoctorRepository`, `DoctorDto`는 doctor 패키지 임시 생성본 — B 작업자 정식 구현 시 교체 예정
- 슬롯 조회: `SlotService.getAvailableSlots()` 호출 (직접 구현 금지)
- 예약번호: `ReservationNumberGenerator` 사용
- CSRF: 모든 POST 폼 및 AJAX에 CSRF 토큰 필수
- Flatpickr: 의사 진료 요일 기반 날짜 제한 (로컬 서빙 `/js/flatpickr.min.js`)

## 테스트

- `@WebMvcTest(ReservationController.class)` + `@MockBean`
- `@DataJpaTest` for Repository

## 의존성

- 내부: `domain/`, `common/service/SlotService`, `doctor/DoctorDto(임시)`
- 외부: Mustache, Spring Security (CSRF), Flatpickr
