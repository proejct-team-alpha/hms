# W2-4 Workflow — 예약 저장 및 완료 화면 연결

> **작성일**: 2W
> **목표**: 예약 폼 제출 시 Patient·Reservation DB 저장 + PRG 패턴 완료 화면 연결

---

## 전체 흐름

```
POST /reservation/create
  → Patient 조회/생성 → Reservation 저장
  → redirect:/reservation/complete (PRG 패턴)
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 예약 폼 제출 → DB 저장 → 완료 화면 |
| Patient 처리 | phone으로 조회, 없으면 신규 생성 |
| PRG 패턴 | POST 저장 후 GET 완료 화면으로 redirect |
| 예약번호 | `R` + timestamp (임시) |

---

## 실행 흐름

```
[1] PatientRepository — findByPhone 추가
[2] ReservationRepository — JpaRepository 구현
[3] DepartmentRepository — 신규 생성
[4] ReservationCreateForm — 폼 DTO 생성
[5] ReservationService — createReservation() 추가
[6] ReservationController — POST /reservation/create 추가
[7] reservation-complete.mustache — 버튼 링크 수정
```

---

## UI Mockup

```
[예약 완료 화면]

┌─────────────────────────────────┐
│  예약이 완료되었습니다           │
├─────────────────────────────────┤
│ 환자명: 홍길동                   │
│ 진료과: 내과                     │
│ 전문의: 의사이영희               │
│ 날짜:   2026-03-20               │
│ 시간:   09:00                    │
│                                  │
│ [메인으로 가기]                  │  → /
└─────────────────────────────────┘
```

---

## 작업 목록

1. `PatientRepository.java` — JpaRepository 구현 + `findByPhone` 추가
2. `ReservationRepository.java` — JpaRepository 구현
3. `DepartmentRepository.java` — 신규 생성
4. `ReservationCreateForm.java` — 폼 DTO 신규 생성
5. `ReservationService.java` — `createReservation()` 추가
6. `ReservationController.java` — `POST /reservation/create` 추가
7. `reservation-complete.mustache` — 버튼 링크 `.html` → 라우팅 경로 수정

---

## 작업 진행내용

- [x] PatientRepository findByPhone 추가
- [x] ReservationRepository 구현
- [x] DepartmentRepository 신규 생성
- [x] ReservationCreateForm 신규 생성
- [x] ReservationService createReservation() 추가
- [x] ReservationController POST /reservation/create 추가
- [x] reservation-complete.mustache 버튼 링크 수정

---

## 실행 흐름에 대한 코드

### PatientRepository

```java
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhone(String phone);
}
```

### ReservationCreateForm

```java
@Getter @Setter
public class ReservationCreateForm {
    private String name;
    private String phone;
    private Long departmentId;
    private Long doctorId;
    private LocalDate reservationDate;
    private String timeSlot;
}
```

### ReservationService — createReservation()

```java
@Transactional
public Reservation createReservation(ReservationCreateForm form) {
    // 1. Patient: phone으로 조회, 없으면 신규 생성
    Patient patient = patientRepository.findByPhone(form.getPhone())
        .orElseGet(() -> patientRepository.save(
            Patient.create(form.getName(), form.getPhone(), null)));
    // 2. Doctor, Department 조회
    Doctor doctor = doctorRepository.findById(form.getDoctorId()).orElseThrow();
    Department dept = departmentRepository.findById(form.getDepartmentId()).orElseThrow();
    // 3. 예약번호 생성 (R + 타임스탬프)
    String reservationNumber = "R" + System.currentTimeMillis();
    // 4. Reservation 저장
    return reservationRepository.save(
        Reservation.create(reservationNumber, patient, doctor, dept,
            form.getReservationDate(), form.getTimeSlot(), ReservationSource.ONLINE));
}
```

### ReservationController — POST /reservation/create

```java
@PostMapping("/create")
public String createReservation(ReservationCreateForm form, RedirectAttributes ra) {
    Reservation r = reservationService.createReservation(form);
    ra.addAttribute("name",       r.getPatient().getName());
    ra.addAttribute("department", r.getDepartment().getName());
    ra.addAttribute("doctor",     r.getDoctor().getStaff().getName());
    ra.addAttribute("date",       r.getReservationDate().toString());
    ra.addAttribute("time",       r.getTimeSlot());
    return "redirect:/reservation/complete";
}
```

### reservation-complete.mustache — 버튼 링크

```html
<!-- 변경 전 -->
<button onclick="location.href='../index.html'">메인으로 가기</button>
<!-- 변경 후 -->
<button onclick="location.href='/'">메인으로 가기</button>
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 신규 환자 예약 | 미등록 전화번호 | Patient 신규 생성 + Reservation 저장 |
| 기존 환자 예약 | 등록된 전화번호 | 기존 Patient 조회 + Reservation 저장 |
| PRG 패턴 | POST 완료 후 | redirect:/reservation/complete |
| 완료 화면 | URL 파라미터 확인 | name/dept/doctor/date/time 표시 |

---

## 완료 기준

- [x] Patient phone으로 조회/생성 정상 동작
- [x] Reservation DB 저장 완료
- [x] PRG 패턴 redirect 동작
- [x] 완료 화면 예약 정보 표시
