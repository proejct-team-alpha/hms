# W2-#5 Workflow: ReservationService 비즈니스 로직 구현

## 개요

예약 생성 중복 방지, 예약 조회(서비스 레이어), 취소(상태 변경), 변경(cancel + create)을
서비스/컨트롤러/화면 레벨로 구현한다.

---

## 실행 흐름

```
[1] ReservationRepository.java   — 쿼리 메서드 3개 추가
[2] ReservationUpdateForm.java   — 변경 폼 DTO (신규 생성)
[3] ReservationService.java      — createReservation() 중복 체크 + 4개 메서드 추가
[4] ReservationController.java   — GET/POST 취소·변경 엔드포인트 추가
[5] reservation-cancel.mustache  — 취소 확인 화면 (신규 생성)
[6] reservation-modify.mustache  — 변경 폼 화면 (신규 생성)
```

---

## 실행 흐름에 대한 코드

### [1] ReservationRepository.java — 쿼리 메서드 추가

```java
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // 예약번호로 단건 조회
    Optional<Reservation> findByReservationNumber(String reservationNumber);

    // 전화번호 + 이름으로 목록 조회
    List<Reservation> findByPatient_PhoneAndPatient_Name(String phone, String name);

    // 중복 예약 체크 (CANCELLED 제외)
    boolean existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
            Long doctorId, LocalDate reservationDate, String timeSlot, ReservationStatus status);
}
```

---

### [2] ReservationUpdateForm.java (신규 생성)

```java
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter
public class ReservationUpdateForm {
    private Long departmentId;
    private Long doctorId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reservationDate;
    private String timeSlot;
}
```

---

### [3] ReservationService.java — 메서드 추가

#### createReservation() — 중복 체크 추가

```java
@Transactional
public ReservationCompleteInfo createReservation(ReservationCreateForm form) {
    // 중복 예약 체크 (CANCELLED 제외)
    if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
            form.getDoctorId(), form.getReservationDate(), form.getTimeSlot(), ReservationStatus.CANCELLED)) {
        throw new IllegalStateException("이미 예약된 시간대입니다.");
    }

    // 이하 기존 로직 동일
    Patient patient = patientRepository.findByPhone(form.getPhone())
            .orElseGet(() -> patientRepository.save(
                    Patient.create(form.getName(), form.getPhone(), null)));
    // ... (기존 코드 유지)
}
```

#### 예약 조회 메서드

```java
// 예약번호로 단건 조회
public Optional<Reservation> findByReservationNumber(String reservationNumber) {
    return reservationRepository.findByReservationNumber(reservationNumber);
}

// 전화번호 + 이름으로 목록 조회
public List<Reservation> findByPhoneAndName(String phone, String name) {
    return reservationRepository.findByPatient_PhoneAndPatient_Name(phone, name);
}
```

#### 예약 취소

```java
@Transactional
public void cancelReservation(Long id) {
    Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
    reservation.cancel();
}
```

#### 예약 변경 (cancel + create)

```java
@Transactional
public ReservationCompleteInfo updateReservation(Long id, ReservationUpdateForm form) {
    // 1. 기존 예약 취소
    Reservation old = reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));
    Patient patient = old.getPatient();
    old.cancel();

    // 2. 새 슬롯 중복 체크
    if (reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
            form.getDoctorId(), form.getReservationDate(), form.getTimeSlot(), ReservationStatus.CANCELLED)) {
        throw new IllegalStateException("이미 예약된 시간대입니다.");
    }

    // 3. 신규 예약 생성
    Doctor doctor = doctorRepository.findById(form.getDoctorId())
            .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
    Department department = departmentRepository.findById(form.getDepartmentId())
            .orElseThrow(() -> new IllegalArgumentException("진료과를 찾을 수 없습니다."));

    String reservationNumber = "R" + System.currentTimeMillis();
    Reservation newReservation = Reservation.create(
            reservationNumber, patient, doctor, department,
            form.getReservationDate(), form.getTimeSlot(), ReservationSource.ONLINE
    );
    reservationRepository.save(newReservation);

    return new ReservationCompleteInfo(
            patient.getName(),
            department.getName(),
            doctor.getStaff().getName(),
            form.getReservationDate().toString(),
            form.getTimeSlot()
    );
}
```

---

### [4] ReservationController.java — 취소·변경 엔드포인트 추가

```java
// 취소 화면 (GET): reservationNumber 파라미터로 예약 조회
@GetMapping("/cancel")
public String cancelPage(HttpServletRequest request) {
    String reservationNumber = request.getParameter("reservationNumber");
    if (reservationNumber != null && !reservationNumber.isBlank()) {
        reservationService.findByReservationNumber(reservationNumber)
                .ifPresentOrElse(
                        r -> request.setAttribute("reservation", r),
                        () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                );
    }
    request.setAttribute("pageTitle", "예약 취소");
    return "reservation/reservation-cancel";
}

// 취소 처리 (POST): PRG 패턴
@PostMapping("/cancel/{id}")
public String cancelReservation(@PathVariable Long id) {
    reservationService.cancelReservation(id);
    return "redirect:/reservation";
}

// 변경 화면 (GET): reservationNumber 파라미터로 예약 조회
@GetMapping("/modify")
public String modifyPage(HttpServletRequest request) {
    String reservationNumber = request.getParameter("reservationNumber");
    if (reservationNumber != null && !reservationNumber.isBlank()) {
        reservationService.findByReservationNumber(reservationNumber)
                .ifPresentOrElse(
                        r -> request.setAttribute("reservation", r),
                        () -> request.setAttribute("errorMessage", "예약을 찾을 수 없습니다.")
                );
    }
    request.setAttribute("pageTitle", "예약 변경");
    return "reservation/reservation-modify";
}

// 변경 처리 (POST): PRG 패턴
@PostMapping("/modify/{id}")
public String modifyReservation(@PathVariable Long id,
                                @ModelAttribute ReservationUpdateForm form,
                                RedirectAttributes redirectAttributes) {
    ReservationCompleteInfo info = reservationService.updateReservation(id, form);
    redirectAttributes.addAttribute("name",       info.getPatientName());
    redirectAttributes.addAttribute("department", info.getDepartmentName());
    redirectAttributes.addAttribute("doctor",     info.getDoctorName());
    redirectAttributes.addAttribute("date",       info.getReservationDate());
    redirectAttributes.addAttribute("time",       info.getTimeSlot());
    return "redirect:/reservation/complete";
}
```

---

### [5] reservation-cancel.mustache (신규 생성)

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>예약 취소 - MediCare+</title>
  <link rel="stylesheet" href="/css/style.css">
  <script src="/js/feather.min.js"></script>
</head>
<body class="min-h-screen flex flex-col">
  {{> common/header-public}}
  <main class="flex-1 overflow-y-auto p-8">
    <div class="max-w-2xl mx-auto">

      {{! 예약번호 검색 폼 }}
      <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 mb-6">
        <h1 class="text-2xl font-bold text-slate-800 mb-6">예약 취소</h1>
        <form method="get" action="/reservation/cancel" class="flex gap-3">
          <input type="text" name="reservationNumber" placeholder="예약번호 입력 (예: R1234567890)"
                 class="flex-1 border border-slate-300 rounded-lg px-4 py-2 text-sm" required />
          <button type="submit"
                  class="px-6 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700">
            조회
          </button>
        </form>
        {{#errorMessage}}
        <p class="mt-3 text-sm text-red-500">{{errorMessage}}</p>
        {{/errorMessage}}
      </div>

      {{! 예약 상세 + 취소 확인 }}
      {{#reservation}}
      <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <h2 class="text-lg font-semibold text-slate-800 mb-6">예약 정보 확인</h2>
        <div class="space-y-3 text-sm text-slate-700 mb-8">
          <div class="flex justify-between"><span class="text-slate-500">예약번호</span><span>{{reservationNumber}}</span></div>
          <div class="flex justify-between"><span class="text-slate-500">환자명</span><span>{{patient.name}}</span></div>
          <div class="flex justify-between"><span class="text-slate-500">진료과</span><span>{{department.name}}</span></div>
          <div class="flex justify-between"><span class="text-slate-500">전문의</span><span>{{doctor.staff.name}}</span></div>
          <div class="flex justify-between"><span class="text-slate-500">예약 일시</span><span>{{reservationDate}} {{timeSlot}}</span></div>
          <div class="flex justify-between"><span class="text-slate-500">상태</span><span>{{status}}</span></div>
        </div>
        <form method="post" action="/reservation/cancel/{{id}}">
          <button type="submit"
                  class="w-full py-3 bg-red-500 text-white font-medium rounded-xl hover:bg-red-600 transition-colors">
            예약 취소
          </button>
        </form>
      </div>
      {{/reservation}}

    </div>
  </main>
  {{> common/footer-public}}
  <script>feather.replace();</script>
</body>
</html>
```

---

### [6] reservation-modify.mustache (신규 생성)

```html
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>예약 변경 - MediCare+</title>
  <link rel="stylesheet" href="/css/style.css">
  <script src="/js/feather.min.js"></script>
  <link rel="stylesheet" href="/css/flatpickr.min.css">
  <script src="/js/flatpickr.min.js"></script>
</head>
<body class="min-h-screen flex flex-col">
  {{> common/header-public}}
  <main class="flex-1 overflow-y-auto p-8">
    <div class="max-w-2xl mx-auto">

      {{! 예약번호 검색 폼 }}
      <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-8 mb-6">
        <h1 class="text-2xl font-bold text-slate-800 mb-6">예약 변경</h1>
        <form method="get" action="/reservation/modify" class="flex gap-3">
          <input type="text" name="reservationNumber" placeholder="예약번호 입력 (예: R1234567890)"
                 class="flex-1 border border-slate-300 rounded-lg px-4 py-2 text-sm" required />
          <button type="submit"
                  class="px-6 py-2 bg-indigo-600 text-white rounded-lg text-sm font-medium hover:bg-indigo-700">
            조회
          </button>
        </form>
        {{#errorMessage}}
        <p class="mt-3 text-sm text-red-500">{{errorMessage}}</p>
        {{/errorMessage}}
      </div>

      {{! 변경 폼 }}
      {{#reservation}}
      <div class="bg-white rounded-2xl shadow-sm border border-slate-200 p-8">
        <h2 class="text-lg font-semibold text-slate-800 mb-2">변경할 정보 입력</h2>
        <p class="text-sm text-slate-500 mb-6">현재 예약: {{department.name}} / {{doctor.staff.name}} / {{reservationDate}} {{timeSlot}}</p>
        <form method="post" action="/reservation/modify/{{id}}" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">진료과</label>
            <select id="department" name="departmentId" required
                    class="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm">
              <option value="">선택해주세요</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">전문의</label>
            <select id="doctor" name="doctorId" required
                    class="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm">
              <option value="">진료과를 먼저 선택하세요</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">날짜</label>
            <input type="text" id="date" name="reservationDate" required readonly
                   placeholder="날짜를 선택해주세요"
                   class="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm bg-white" />
          </div>
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-1">시간대</label>
            <select id="timeSlot" name="timeSlot" required
                    class="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm">
              <option value="">선택해주세요</option>
              <option value="09:00">09:00</option>
              <option value="09:30">09:30</option>
              <option value="10:00">10:00</option>
              <option value="10:30">10:30</option>
              <option value="11:00">11:00</option>
              <option value="11:30">11:30</option>
              <option value="14:00">14:00</option>
              <option value="14:30">14:30</option>
              <option value="15:00">15:00</option>
              <option value="15:30">15:30</option>
              <option value="16:00">16:00</option>
              <option value="16:30">16:30</option>
            </select>
          </div>
          <button type="submit"
                  class="w-full py-3 bg-indigo-600 text-white font-medium rounded-xl hover:bg-indigo-700 transition-colors">
            예약 변경
          </button>
        </form>
      </div>
      {{/reservation}}

    </div>
  </main>
  {{> common/footer-public}}
  <script>
    feather.replace();

    const DAY_MAP = { SUN: 0, MON: 1, TUE: 2, WED: 3, THU: 4, FRI: 5, SAT: 6 };
    const datePicker = flatpickr(document.getElementById('date'), {
      dateFormat: 'Y-m-d',
      animate: false,
      disable: [() => true]
    });

    // 진료과 목록 로드
    fetch('/api/reservation/departments')
      .then(res => res.json())
      .then(depts => {
        const sel = document.getElementById('department');
        depts.forEach(d => {
          const opt = document.createElement('option');
          opt.value = d.id;
          opt.textContent = d.name;
          sel.appendChild(opt);
        });
      });

    document.getElementById('department').addEventListener('change', function () {
      const departmentId = this.value;
      const doctorSelect = document.getElementById('doctor');
      doctorSelect.innerHTML = '<option value="">선택해주세요</option>';
      datePicker.clear();
      if (!departmentId) return;
      fetch(`/api/reservation/doctors?departmentId=${departmentId}`)
        .then(res => res.json())
        .then(doctors => {
          doctors.forEach(d => {
            const opt = document.createElement('option');
            opt.value = d.id;
            opt.textContent = d.name;
            opt.dataset.availableDays = d.availableDays;
            doctorSelect.appendChild(opt);
          });
        });
    });

    document.getElementById('doctor').addEventListener('change', function () {
      const availableDaysStr = this.options[this.selectedIndex].dataset.availableDays || '';
      const availableDayNums = availableDaysStr.split(',').map(d => DAY_MAP[d.trim()]).filter(n => n !== undefined);
      datePicker.clear();
      datePicker.set('disable', [date => !availableDayNums.includes(date.getDay())]);
    });
  </script>
</body>
</html>
```

---

## 작업 파일

| 파일 | 작업 |
|------|------|
| `reservation/reservation/ReservationRepository.java` | 쿼리 메서드 3개 추가 |
| `reservation/reservation/ReservationUpdateForm.java` | 신규 생성 |
| `reservation/reservation/ReservationService.java` | 중복 체크 + 4개 메서드 추가 |
| `reservation/reservation/ReservationController.java` | 취소·변경 GET/POST 4개 추가 |
| `templates/reservation/reservation-cancel.mustache` | 신규 생성 |
| `templates/reservation/reservation-modify.mustache` | 신규 생성 |

---

## 참고: 변경 방식 설계 결정

`domain/Reservation.java` 는 금지 영역으로 필드 업데이트 메서드를 추가할 수 없다.
따라서 예약 변경은 **기존 예약 취소(cancel) + 신규 예약 생성(create)** 조합으로 처리한다.

> **참고**: `reservation` 테이블에 `(doctor_id, reservation_date, time_slot)` 유니크 제약이 있어,
> 동일 슬롯으로 변경하는 경우 DB 레벨 오류가 발생할 수 있다. (교육용 샘플이므로 생략)
