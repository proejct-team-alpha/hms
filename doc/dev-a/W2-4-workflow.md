# W2-#4 Workflow: 예약 저장 및 완료 화면 연결

## 개요

예약 폼 제출 시 Patient·Reservation을 DB에 저장하고,
PRG 패턴으로 예약 완료 화면에 예약 요약 정보를 전달한다.

---

## 실행 흐름

```
[1] PatientRepository.java — JpaRepository 구현 + findByPhone 추가
[2] ReservationRepository.java — JpaRepository 구현
[3] DepartmentRepository.java — JpaRepository 구현 (신규 생성)
[4] ReservationCreateForm.java — 폼 DTO 생성 (신규 생성)
[5] ReservationService.java — createReservation() 메서드 추가
[6] ReservationController.java — POST /reservation/create 추가
[7] reservation-complete.mustache — 버튼 링크 수정 (html → 라우팅 경로)
```

---

## 실행 흐름에 대한 코드

### [1] PatientRepository.java

```java
import com.smartclinic.hms.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhone(String phone);
}
```

---

### [2] ReservationRepository.java

```java
import com.smartclinic.hms.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
```

---

### [3] DepartmentRepository.java (신규 생성)

```java
import com.smartclinic.hms.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
```

---

### [4] ReservationCreateForm.java (신규 생성)

```java
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

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

### [5] ReservationService.java — createReservation() 추가

```java
// 필드 추가
private final PatientRepository patientRepository;
private final ReservationRepository reservationRepository;
private final DepartmentRepository departmentRepository;
private final DoctorRepository doctorRepository; // 기존 doctor 패키지 빈 주입

@Transactional
public com.smartclinic.hms.domain.Reservation createReservation(ReservationCreateForm form) {
    // 1. Patient: phone으로 조회, 없으면 신규 생성
    Patient patient = patientRepository.findByPhone(form.getPhone())
        .orElseGet(() -> patientRepository.save(Patient.create(form.getName(), form.getPhone(), null)));

    // 2. Doctor, Department 조회
    Doctor doctor = doctorRepository.findById(form.getDoctorId())
        .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
    Department department = departmentRepository.findById(form.getDepartmentId())
        .orElseThrow(() -> new IllegalArgumentException("진료과를 찾을 수 없습니다."));

    // 3. 예약번호 생성 (R + 14자리 타임스탬프)
    String reservationNumber = "R" + System.currentTimeMillis();

    // 4. Reservation 생성 및 저장
    com.smartclinic.hms.domain.Reservation reservation = com.smartclinic.hms.domain.Reservation.create(
        reservationNumber, patient, doctor, department,
        form.getReservationDate(), form.getTimeSlot(),
        com.smartclinic.hms.domain.ReservationSource.ONLINE
    );
    return reservationRepository.save(reservation);
}
```

---

### [6] ReservationController.java — POST /reservation/create 추가

```java
// PRG 패턴: POST 저장 후 GET 완료 화면으로 리다이렉트
@PostMapping("/create")
public String createReservation(ReservationCreateForm form, RedirectAttributes redirectAttributes) {
    com.smartclinic.hms.domain.Reservation reservation = reservationService.createReservation(form);

    redirectAttributes.addAttribute("name",       reservation.getPatient().getName());
    redirectAttributes.addAttribute("department", reservation.getDepartment().getName());
    redirectAttributes.addAttribute("doctor",     reservation.getDoctor().getStaff().getName());
    redirectAttributes.addAttribute("date",       reservation.getReservationDate().toString());
    redirectAttributes.addAttribute("time",       reservation.getTimeSlot());

    return "redirect:/reservation/complete";
}
```

---

### [7] reservation-complete.mustache — 버튼 링크 수정

```html
<!-- 기존: .html 경로 -->
<button onclick="location.href='../index.html'">메인으로 가기</button>

<!-- 변경: 라우팅 경로 -->
<button onclick="location.href='/'">메인으로 가기</button>
```

---

## 작업 파일

| 파일 | 작업 |
|------|------|
| `reservation/reservation/PatientRepository.java` | JpaRepository 구현 + findByPhone |
| `reservation/reservation/ReservationRepository.java` | JpaRepository 구현 |
| `reservation/reservation/DepartmentRepository.java` | 신규 생성 |
| `reservation/reservation/ReservationCreateForm.java` | 신규 생성 |
| `reservation/reservation/ReservationService.java` | createReservation() 추가 |
| `reservation/reservation/ReservationController.java` | POST /reservation/create 추가 |
| `templates/reservation/reservation-complete.mustache` | 버튼 링크 수정 |
