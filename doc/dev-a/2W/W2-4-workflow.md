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


| 항목         | 내용                             |
| ---------- | ------------------------------ |
| 요청         | 예약 폼 제출 → DB 저장 → 완료 화면        |
| Patient 처리 | phone으로 조회, 없으면 신규 생성          |
| PRG 패턴     | POST 저장 후 GET 완료 화면으로 redirect |
| 예약번호       | `R` + timestamp (임시)           |


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

- PatientRepository findByPhone 추가
- ReservationRepository 구현
- DepartmentRepository 신규 생성
- ReservationCreateForm 신규 생성
- ReservationService createReservation() 추가
- ReservationController POST /reservation/create 추가
- reservation-complete.mustache 버튼 링크 수정

---

## 실행 흐름에 대한 코드

### PatientRepository

```java
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByPhone(String phone);
}
```

> **💡 입문자 설명**
>
> - **이 코드가 하는 일**: 환자 테이블에서 전화번호로 환자를 조회하는 리포지토리입니다. `Optional`은 조회 결과가 없을 수도 있음을 나타내는 Java 클래스입니다.
> - **왜 이렇게 썼는지**: 전화번호는 환자를 구분하는 식별자로 사용됩니다. `Optional`을 반환하면 결과가 없을 때 null 대신 `Optional.empty()`를 반환하여 NullPointerException을 방지할 수 있습니다.
> - **쉽게 말하면**: 전화번호부에서 특정 번호를 찾아주는 검색 기능입니다. 없으면 "없음"을 안전하게 알려줍니다.

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

> **💡 입문자 설명**
>
> - **이 코드가 하는 일**: 예약 폼에서 제출된 데이터를 받아 담는 데이터 전달 객체입니다. HTML 폼의 `name` 속성과 이 클래스의 필드명이 일치할 때 Spring이 자동으로 값을 채워줍니다.
> - **왜 이렇게 썼는지**: `@Getter`는 값을 읽는 메서드를, `@Setter`는 값을 쓰는 메서드를 자동 생성합니다. `@Setter`가 있어야 Spring이 폼 데이터를 이 객체에 주입할 수 있습니다. `LocalDate`는 날짜만 다루는 Java 클래스입니다.
> - **쉽게 말하면**: 예약 폼 종이를 그대로 받아서 담아두는 빈 서류 양식입니다.

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

> **💡 입문자 설명**
>
> - **이 코드가 하는 일**: 예약 폼 데이터로 환자(Patient)와 예약(Reservation)을 DB에 저장합니다. 전화번호로 기존 환자를 찾고, 없으면 신규 환자를 만듭니다.
> - **왜 이렇게 썼는지**: `@Transactional`은 이 메서드 안의 모든 DB 작업이 하나의 묶음으로 처리됨을 보장합니다(중간에 오류 나면 전부 취소). `.orElseGet()`은 Optional에 값이 없을 때 대신 실행할 코드를 지정하는 방법입니다.
> - **쉽게 말하면**: 예약 접수 직원이 전화번호로 기존 환자를 찾아보고, 처음 오는 환자면 새로 등록한 뒤 예약을 DB에 저장하는 과정입니다.

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

> **💡 입문자 설명**
>
> - **이 코드가 하는 일**: POST로 예약 데이터를 받아 저장한 뒤, 완료 화면으로 리다이렉트합니다. 완료 화면에 보여줄 예약 정보는 URL 파라미터로 전달합니다.
> - **왜 이렇게 썼는지**: PRG(Post-Redirect-Get) 패턴입니다. POST 처리 후 바로 뷰를 반환하면, 사용자가 새로고침할 때 폼이 다시 제출되는 문제가 생깁니다. `redirect:`를 반환하면 브라우저가 GET 요청으로 완료 페이지에 접근하게 되어 중복 제출을 방지합니다. `RedirectAttributes`는 리다이렉트 시 파라미터를 전달하는 객체입니다.
> - **쉽게 말하면**: 예약 접수 후 "예약 완료 페이지로 이동하세요"라고 안내하는 것입니다. 예약 정보는 이동할 주소 뒤에 붙여서 전달합니다.

### reservation-complete.mustache — 버튼 링크

```html
<!-- 변경 전 -->
<button onclick="location.href='../index.html'">메인으로 가기</button>
<!-- 변경 후 -->
<button onclick="location.href='/'">메인으로 가기</button>
```

> **💡 입문자 설명**
>
> - **이 코드가 하는 일**: "메인으로 가기" 버튼을 클릭하면 사이트 최상위 경로(`/`)로 이동합니다.
> - **왜 이렇게 썼는지**: 기존 코드는 정적 HTML 파일 기준의 상대 경로(`../index.html`)를 사용했습니다. Spring MVC로 전환한 후에는 라우팅 경로(`/`)를 사용해야 서버가 올바르게 메인 페이지 컨트롤러로 연결합니다.
> - **쉽게 말하면**: 건물 안내판에서 "2층 복도 끝 왼쪽"이라는 복잡한 길 안내 대신 "로비로 가세요"라고 간단하게 바꾼 것입니다.

---

## 테스트 진행


| 케이스      | 조건          | 기대 결과                          |
| -------- | ----------- | ------------------------------ |
| 신규 환자 예약 | 미등록 전화번호    | Patient 신규 생성 + Reservation 저장 |
| 기존 환자 예약 | 등록된 전화번호    | 기존 Patient 조회 + Reservation 저장 |
| PRG 패턴   | POST 완료 후   | redirect:/reservation/complete |
| 완료 화면    | URL 파라미터 확인 | name/dept/doctor/date/time 표시  |


---

## 완료 기준

- Patient phone으로 조회/생성 정상 동작
- Reservation DB 저장 완료
- PRG 패턴 redirect 동작
- 완료 화면 예약 정보 표시

