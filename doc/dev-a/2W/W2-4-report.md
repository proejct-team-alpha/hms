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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 환자 DB 테이블에 대한 CRUD 기능과, 전화번호로 환자를 조회하는 기능을 제공하는 인터페이스입니다.
> - **왜 이렇게 썼는지**: `JpaRepository<Patient, Long>`을 상속받으면 기본 저장/조회/삭제 등의 메서드가 자동으로 생성됩니다. `findByPhone`은 Spring Data JPA의 메서드 이름 규칙으로 자동 쿼리가 생성됩니다.
> - **쉽게 말하면**: 환자 관리 창구를 만들어, "전화번호로 환자 찾기" 기능을 추가한 것입니다.

---

### 2. ReservationRepository

기본 CRUD용 JpaRepository 구현.

```java
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 테이블에 대한 기본 CRUD 기능을 제공하는 리포지토리입니다. 추가 메서드 없이 기본 기능만 사용합니다.
> - **왜 이렇게 썼는지**: `JpaRepository`를 상속받는 것만으로 `save()`, `findById()`, `findAll()`, `delete()` 등 예약에 필요한 기본 DB 연산이 모두 자동으로 제공됩니다.
> - **쉽게 말하면**: 예약 장부 담당 직원을 고용했는데, 기본 업무(저장, 조회, 삭제)는 별도 교육 없이 자동으로 할 수 있습니다.

---

### 3. DepartmentRepository (신규)

`createReservation()` 에서 진료과 엔티티 조회에 사용.

```java
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 진료과(Department) 테이블에 대한 기본 CRUD 리포지토리입니다. 예약 생성 시 진료과 엔티티를 ID로 조회하는 데 사용됩니다.
> - **왜 이렇게 썼는지**: `createReservation()` 서비스 메서드에서 `departmentRepository.findById(id)`로 진료과를 조회해야 하므로 새로 생성했습니다.
> - **쉽게 말하면**: 진료과 정보를 DB에서 꺼내오는 전담 창구를 새로 만든 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 폼에서 제출된 데이터를 담는 DTO 클래스입니다. HTML 폼의 각 `name` 속성과 이 클래스의 필드명이 1:1로 대응됩니다.
> - **왜 이렇게 썼는지**: Spring MVC가 POST 요청의 폼 데이터를 이 객체에 자동으로 채워줍니다(`@Setter` 필요). `@Getter`는 서비스에서 값을 읽을 때 사용됩니다. `LocalDate`는 시간 없이 날짜만 다루는 Java 8 날짜 클래스입니다.
> - **쉽게 말하면**: 예약 신청서 양식을 Java 클래스로 표현한 것입니다. 폼을 제출하면 이 양식에 자동으로 값이 채워집니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 폼 데이터로 환자 조회/생성, 의사와 진료과 조회, 예약번호 생성 후 예약을 DB에 저장하는 서비스 메서드입니다.
> - **왜 이렇게 썼는지**: `@Transactional`로 전체 과정을 하나의 트랜잭션으로 묶어, 중간에 오류가 나면 모두 롤백됩니다. `orElseThrow()`는 조회 결과가 없을 때 예외를 던져 잘못된 ID 입력을 막습니다. `System.currentTimeMillis()`는 현재 시각의 밀리초 값으로 유일한 번호를 만듭니다.
> - **쉽게 말하면**: 예약 접수원이 전화번호로 기존 환자를 찾고(없으면 신규 등록), 의사와 진료과를 확인한 뒤 예약을 접수하는 과정입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 폼을 POST로 받아 저장한 뒤, 완료 화면으로 리다이렉트하면서 예약 요약 정보를 URL 파라미터로 전달합니다.
> - **왜 이렇게 썼는지**: PRG(Post-Redirect-Get) 패턴을 적용했습니다. 저장 후 바로 화면을 반환하면 브라우저 새로고침 시 폼이 다시 제출되는 문제가 있습니다. `redirect:`로 응답하면 브라우저가 새로운 GET 요청을 보내므로 중복 제출이 방지됩니다. `RedirectAttributes.addAttribute()`는 리다이렉트 URL에 쿼리 파라미터를 붙입니다.
> - **쉽게 말하면**: 예약을 받고 나서 "완료 페이지로 이동하세요"라는 안내장을 주고, 이름·진료과·날짜·시간 정보는 안내장 뒤에 메모해서 보내는 것입니다.

---

### 7. reservation-complete.mustache — 버튼 링크 수정

```html
<!-- 기존 -->
<button onclick="location.href='../index.html'">메인으로 가기</button>

<!-- 변경 -->
<button onclick="location.href='/'">메인으로 가기</button>
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: "메인으로 가기" 버튼 클릭 시 이동하는 주소를 변경합니다.
> - **왜 이렇게 썼는지**: 기존 코드는 정적 파일 기준의 상대 경로(`../index.html`)입니다. Spring MVC 프로젝트에서는 서버 라우팅 경로(`/`)를 사용해야 컨트롤러를 통해 올바르게 메인 페이지로 이동됩니다.
> - **쉽게 말하면**: 예전 주소 체계(파일 경로)에서 새 주소 체계(서버 경로)로 바꿔 링크가 올바르게 동작하도록 수정한 것입니다.

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
