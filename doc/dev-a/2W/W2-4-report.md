# W2-#4 Report: 예약 저장 및 완료 화면 연결

## 작업 개요

예약 폼 제출 시 Patient·Reservation을 DB에 저장하고,
PRG 패턴으로 예약 완료 화면에 예약 요약 정보를 전달.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `reservation/PatientRepository.java` | JpaRepository 구현 + `findByPhone` 추가 |
| `reservation/ReservationRepository.java` | JpaRepository 구현 |
| `reservation/DepartmentRepository.java` | 신규 생성 — JpaRepository 구현 |
| `reservation/ReservationCreateForm.java` | 신규 생성 — 폼 DTO |
| `reservation/ReservationService.java` | `createReservation()` 추가 |
| `reservation/ReservationController.java` | `POST /reservation/create` 추가 |
| `templates/reservation/reservation-complete.mustache` | 버튼 링크 `.html` → 라우팅 경로로 수정 |

---

## 구현 상세

### 1. PatientRepository — findByPhone 추가

전화번호로 기존 환자를 조회해 중복 등록을 방지.

```java
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhone(String phone);
}
```

---

### 2. ReservationRepository

기본 CRUD용 JpaRepository 구현.

```java
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
```

---

### 3. DepartmentRepository (신규)

`createReservation()` 에서 진료과 엔티티 조회에 사용.

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
```

---

### 4. ReservationCreateForm (신규)

폼 데이터를 바인딩받는 DTO.

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

---

### 5. ReservationService — createReservation()

```java
@Transactional
public Reservation createReservation(ReservationCreateForm form) {
    // 전화번호로 조회, 없으면 신규 Patient 생성
    Patient patient = patientRepository.findByPhone(form.getPhone())
        .orElseGet(() -> patientRepository.save(
            Patient.create(form.getName(), form.getPhone(), null)));

    Doctor doctor = doctorRepository.findById(form.getDoctorId())
        .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
    Department department = departmentRepository.findById(form.getDepartmentId())
        .orElseThrow(() -> new IllegalArgumentException("진료과를 찾을 수 없습니다."));

    // 예약번호: R + 14자리 타임스탬프
    String reservationNumber = "R" + System.currentTimeMillis();

    Reservation reservation = Reservation.create(
        reservationNumber, patient, doctor, department,
        form.getReservationDate(), form.getTimeSlot(),
        ReservationSource.ONLINE
    );
    return reservationRepository.save(reservation);
}
```

---

### 6. ReservationController — POST /reservation/create

PRG 패턴: POST 저장 후 GET 완료 화면으로 리다이렉트.

```java
@PostMapping("/create")
public String createReservation(ReservationCreateForm form, RedirectAttributes redirectAttributes) {
    Reservation reservation = reservationService.createReservation(form);

    redirectAttributes.addAttribute("name",       reservation.getPatient().getName());
    redirectAttributes.addAttribute("department", reservation.getDepartment().getName());
    redirectAttributes.addAttribute("doctor",     reservation.getDoctor().getStaff().getName());
    redirectAttributes.addAttribute("date",       reservation.getReservationDate().toString());
    redirectAttributes.addAttribute("time",       reservation.getTimeSlot());

    return "redirect:/reservation/complete";
}
```

---

### 7. reservation-complete.mustache — 버튼 링크 수정

```html
<!-- 기존 -->
<button onclick="location.href='../index.html'">메인으로 가기</button>

<!-- 변경 -->
<button onclick="location.href='/'">메인으로 가기</button>
```

---

## 동작 흐름

```
폼 제출 (POST /reservation/create)
  └─ phone으로 Patient 조회 → 없으면 신규 생성
  └─ Doctor, Department 엔티티 조회
  └─ 예약번호 생성 (R + timestamp)
  └─ Reservation 저장
  └─ RedirectAttributes로 요약 정보 전달
  └─ redirect:/reservation/complete (PRG)

GET /reservation/complete
  └─ 이름, 진료과, 의사, 날짜, 시간 표시
```

---

## 수용 기준 확인

- [x] 예약 폼 제출 시 Patient·Reservation DB 저장
- [x] 기존 전화번호 환자는 신규 생성 없이 재사용
- [x] PRG 패턴 적용 (새로고침 시 중복 제출 방지)
- [x] 완료 화면에 예약 요약 정보 표시
- [x] 완료 화면 '메인으로 가기' 버튼 라우팅 경로 수정
