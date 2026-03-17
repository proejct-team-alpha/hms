# 예약 불가 시간 슬롯 비활성화 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 의사+날짜 선택 시 이미 예약된 시간 슬롯을 `disabled` + 회색 + "(예약불가)" 텍스트로 표시해 사용자가 선택하지 못하도록 막는다.

**Architecture:** 새 API `GET /api/reservation/booked-slots`가 예약된 슬롯 문자열 목록을 반환하고, 프론트엔드 Flatpickr `onChange` 콜백이 이를 받아 `<select id="time">` 옵션을 업데이트한다. 예약 변경 페이지는 `excludeId`로 현재 수정 중인 예약의 슬롯을 제외해 자신의 슬롯을 재선택할 수 있도록 한다.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA / Hibernate, Mustache, Flatpickr, Gradle

**Spec:** `docs/superpowers/specs/2026-03-17-booked-slots-design.md`

---

## 파일 구조

| 파일 | 변경 유형 | 역할 |
|------|---------|------|
| `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationRepository.java` | 수정 | 예약된 슬롯 조회 쿼리 2개 추가 |
| `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java` | 수정 | `getBookedTimeSlots` 오버로드 2개 추가 |
| `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java` | 수정 | `GET /api/reservation/booked-slots` 엔드포인트 추가 |
| `src/main/resources/templates/reservation/direct-reservation.mustache` | 수정 | 초기화 + `resetSlots()` + Flatpickr `onChange` 슬롯 업데이트 |
| `src/main/resources/templates/reservation/reservation-modify.mustache` | 수정 | 동일 + `excludeId` 전달 + `minDate: 'today'` |
| `src/test/java/com/smartclinic/hms/reservation/reservation/ReservationServiceTest.java` | 수정 | `getBookedTimeSlots` 서비스 테스트 추가 |

---

## Task 1: Repository — 예약된 슬롯 조회 쿼리 추가

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationRepository.java`

- [ ] **Step 1: `findByIdForUpdate` 바로 아래에 두 메서드 추가**

```java
// 직접 예약 페이지용
@Query("SELECT r.timeSlot FROM Reservation r " +
       "WHERE r.doctor.id = :doctorId " +
       "AND r.reservationDate = :date " +
       "AND r.status <> :excluded")
List<String> findBookedTimeSlots(
        @Param("doctorId") Long doctorId,
        @Param("date") LocalDate date,
        @Param("excluded") ReservationStatus excluded);

// 예약 변경 페이지용 (현재 수정 중인 예약 제외)
@Query("SELECT r.timeSlot FROM Reservation r " +
       "WHERE r.doctor.id = :doctorId " +
       "AND r.reservationDate = :date " +
       "AND r.status <> :excluded " +
       "AND r.id <> :excludeId")
List<String> findBookedTimeSlotsExcluding(
        @Param("doctorId") Long doctorId,
        @Param("date") LocalDate date,
        @Param("excluded") ReservationStatus excluded,
        @Param("excludeId") Long excludeId);
```

> 주의: Hibernate는 nullable Long 파라미터를 JPQL `IS NULL`로 바인딩하면 타입 추론 실패로 런타임 예외가 발생한다. 그래서 메서드를 둘로 분리한다.

---

## Task 2: Service — `getBookedTimeSlots` 오버로드 추가 (TDD)

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java`
- Test: `src/test/java/com/smartclinic/hms/reservation/reservation/ReservationServiceTest.java`

- [ ] **Step 1: 실패하는 테스트 작성**

`ReservationServiceTest.java` 에 아래 테스트 2개 추가:

```java
@Test
@DisplayName("예약된 슬롯 조회 - 직접 예약용")
void getBookedTimeSlots_returnsBookedSlots() {
    // given
    given(reservationRepository.findBookedTimeSlots(
            1L, LocalDate.of(2026, 4, 1), ReservationStatus.CANCELLED))
        .willReturn(List.of("09:00", "10:30"));

    // when
    List<String> slots = reservationService.getBookedTimeSlots(1L, LocalDate.of(2026, 4, 1));

    // then
    assertThat(slots).containsExactlyInAnyOrder("09:00", "10:30");
}

@Test
@DisplayName("예약된 슬롯 조회 - 현재 예약 제외 (변경용)")
void getBookedTimeSlots_withExcludeId_excludesCurrentReservation() {
    // given
    given(reservationRepository.findBookedTimeSlotsExcluding(
            1L, LocalDate.of(2026, 4, 1), ReservationStatus.CANCELLED, 5L))
        .willReturn(List.of("10:30"));

    // when
    List<String> slots = reservationService.getBookedTimeSlots(1L, LocalDate.of(2026, 4, 1), 5L);

    // then
    assertThat(slots).containsExactly("10:30");
}
```

imports 추가 필요:
```java
import java.util.List;
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
cd c:/workspace/gitstudy_lab/demo/team-demo/team-project-demo/hms
./gradlew test --tests "com.smartclinic.hms.reservation.reservation.ReservationServiceTest" 2>&1 | tail -20
```

Expected: `getBookedTimeSlots_returnsBookedSlots` FAILED (메서드 없음)

- [ ] **Step 3: Service에 메서드 구현**

`ReservationService.java` 의 `findByPhoneAndName` 메서드 다음에 추가:

```java
public List<String> getBookedTimeSlots(Long doctorId, LocalDate date) {
        return reservationRepository.findBookedTimeSlots(doctorId, date, ReservationStatus.CANCELLED);
}

public List<String> getBookedTimeSlots(Long doctorId, LocalDate date, Long excludeId) {
        return reservationRepository.findBookedTimeSlotsExcluding(
                        doctorId, date, ReservationStatus.CANCELLED, excludeId);
}
```

필요한 import (`java.time.LocalDate`는 이미 있음, `List`도 있음 — 추가 없음):
`ReservationStatus`도 이미 import 됨.

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
./gradlew test --tests "com.smartclinic.hms.reservation.reservation.ReservationServiceTest" 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`, 모든 테스트 PASSED

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/smartclinic/hms/reservation/reservation/ReservationRepository.java
git add src/main/java/com/smartclinic/hms/reservation/reservation/ReservationService.java
git add src/test/java/com/smartclinic/hms/reservation/reservation/ReservationServiceTest.java
git commit -m "feat(reservation): add getBookedTimeSlots to service and repository"
```

---

## Task 3: API Controller — `/api/reservation/booked-slots` 엔드포인트 추가

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java`

- [ ] **Step 1: 엔드포인트 추가**

기존 `getDoctors` 메서드 아래에 추가:

```java
@GetMapping("/booked-slots")
public List<String> getBookedSlots(
        @RequestParam("doctorId") Long doctorId,
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(value = "excludeId", required = false) Long excludeId) {
    if (excludeId != null) {
        return reservationService.getBookedTimeSlots(doctorId, date, excludeId);
    }
    return reservationService.getBookedTimeSlots(doctorId, date);
}
```

imports 추가:
```java
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
```

> `@RequestParam`은 이미 있을 수 있음 — 중복 import 방지

- [ ] **Step 2: 서버 재시작 후 수동 검증**

브라우저 또는 curl로 확인:
```
GET http://localhost:8080/api/reservation/booked-slots?doctorId=1&date=2026-03-18
```
Expected: `[]` 또는 예약된 슬롯 목록 (예: `["09:00"]`)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/smartclinic/hms/reservation/reservation/ReservationApiController.java
git commit -m "feat(reservation): add GET /api/reservation/booked-slots endpoint"
```

---

## Task 4: Frontend — `direct-reservation.mustache` 슬롯 비활성화

**Files:**
- Modify: `src/main/resources/templates/reservation/direct-reservation.mustache`

현재 `<script>` 블록 구조:
1. `feather.replace()`
2. URL 파라미터 파싱 + AI 추천 처리
3. `DEPT_ID_MAP` 구성
4. Flatpickr 초기화 + 의사/날짜 이벤트 핸들러

- [ ] **Step 1: `feather.replace()` 직후에 `data-original-text` 초기화 + `resetSlots()` 추가**

`feather.replace();` 바로 아래에 삽입:

```javascript
// 슬롯 텍스트 원본 저장 (한 번만 실행)
document.querySelectorAll('#time option').forEach(opt => {
  opt.dataset.originalText = opt.textContent;
});

// 슬롯 전체 초기화 (의사/날짜 변경 시 또는 fetch 실패 시 호출)
function resetSlots() {
  document.querySelectorAll('#time option').forEach(opt => {
    opt.disabled = false;
    opt.classList.remove('text-slate-400');
    opt.textContent = opt.dataset.originalText;
  });
}
```

- [ ] **Step 2: Flatpickr 초기화 블록에 `onChange` 콜백 추가**

현재 Flatpickr 초기화 코드:
```javascript
const datePicker = flatpickr(dateInput, {
  dateFormat: 'Y-m-d',
  minDate: 'today',
  animate: false,
  position: 'below',
  disable: [() => true]
});
```

`onChange` 콜백 추가:
```javascript
const datePicker = flatpickr(dateInput, {
  dateFormat: 'Y-m-d',
  minDate: 'today',
  animate: false,
  position: 'below',
  disable: [() => true],
  onChange: async (selectedDates, dateStr) => {
    const doctorId = document.getElementById('doctor').value;
    if (!dateStr || !doctorId) {
      resetSlots();
      return;
    }
    try {
      const res = await fetch(`/api/reservation/booked-slots?doctorId=${doctorId}&date=${dateStr}`);
      if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
      const booked = await res.json();
      document.querySelectorAll('#time option').forEach(opt => {
        const original = opt.dataset.originalText;
        if (!original) return; // placeholder option
        if (booked.includes(original)) {
          opt.disabled = true;
          opt.classList.add('text-slate-400');
          opt.textContent = original + ' (예약불가)';
        } else {
          opt.disabled = false;
          opt.classList.remove('text-slate-400');
          opt.textContent = original;
        }
      });
    } catch (err) {
      console.error('예약 가능 슬롯 조회 실패:', err);
      resetSlots();
    }
  }
});
```

> `placeholder option` (`value=""`, `originalText="선택해주세요"`)은 `booked.includes`에 절대 매칭되지 않으므로 별도 가드 없이 정상 동작한다.

- [ ] **Step 3: 수동 검증**

1. 서버 재시작
2. `/reservation/direct-reservation` 접속
3. 의사 선택 → 날짜 선택 → 시간 드롭다운 확인
   - 예약된 슬롯: 회색 + "(예약불가)" + 클릭 불가
   - 가용 슬롯: 정상 선택 가능
4. 다른 날짜 선택 → 슬롯 목록이 새 날짜 기준으로 갱신되는지 확인
5. 의사 다시 변경 → 슬롯 전체 초기화(enabled) 확인

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/reservation/direct-reservation.mustache
git commit -m "feat(reservation): disable booked time slots in direct-reservation"
```

---

## Task 5: Frontend — `reservation-modify.mustache` 슬롯 비활성화

**Files:**
- Modify: `src/main/resources/templates/reservation/reservation-modify.mustache`

- [ ] **Step 1: `feather.replace()` 직후 + `excludeId` 변수 주입**

현재 `<script>` 블록 시작:
```javascript
feather.replace();

// 예약 조회 후에는 검색 섹션 숨기기
if (new URLSearchParams(window.location.search).has('reservationNumber')) {
```

아래와 같이 수정 (feather 다음에 삽입):
```javascript
feather.replace();

// H-03: 현재 수정 중인 예약 ID (서버에서 주입) — booked-slots 조회 시 제외
const excludeId = {{id}};

// 슬롯 텍스트 원본 저장
document.querySelectorAll('#time option').forEach(opt => {
  opt.dataset.originalText = opt.textContent;
});

// 슬롯 전체 초기화
function resetSlots() {
  document.querySelectorAll('#time option').forEach(opt => {
    opt.disabled = false;
    opt.classList.remove('text-slate-400');
    opt.textContent = opt.dataset.originalText;
  });
}
```

> `{{id}}`는 `{{#reservation}}...{{/reservation}}` 블록 안에 있어 이미 예약 ID가 바인딩된다.
> 변경 폼이 보이는 경우(`{{#canModify}}`)에만 JS가 실행되므로 `excludeId`는 항상 유효한 Long 값이다.

- [ ] **Step 2: Flatpickr 초기화에 `onChange` + `minDate` 추가**

현재:
```javascript
const datePicker = flatpickr(document.getElementById('date'), {
  dateFormat: 'Y-m-d',
  animate: false,
  position: 'below',
  disable: [() => true]
});
```

수정:
```javascript
const datePicker = flatpickr(document.getElementById('date'), {
  dateFormat: 'Y-m-d',
  minDate: 'today',
  animate: false,
  position: 'below',
  disable: [() => true],
  onChange: async (selectedDates, dateStr) => {
    const doctorId = document.getElementById('doctor').value;
    if (!dateStr || !doctorId) {
      resetSlots();
      return;
    }
    try {
      const res = await fetch(
        `/api/reservation/booked-slots?doctorId=${doctorId}&date=${dateStr}&excludeId=${excludeId}`
      );
      if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
      const booked = await res.json();
      document.querySelectorAll('#time option').forEach(opt => {
        const original = opt.dataset.originalText;
        if (!original) return;
        if (booked.includes(original)) {
          opt.disabled = true;
          opt.classList.add('text-slate-400');
          opt.textContent = original + ' (예약불가)';
        } else {
          opt.disabled = false;
          opt.classList.remove('text-slate-400');
          opt.textContent = original;
        }
      });
    } catch (err) {
      console.error('예약 가능 슬롯 조회 실패:', err);
      resetSlots();
    }
  }
});
```

- [ ] **Step 3: 수동 검증**

1. 서버 재시작
2. `/reservation/modify?reservationNumber=RES-...` 로 접속 (예약번호 조회)
3. 같은 의사 + 같은 날짜 선택 시 → 현재 내 슬롯("09:00" 등)이 **활성** 상태인지 확인
4. 다른 날짜 선택 시 → 해당 날짜의 예약된 슬롯이 "(예약불가)"로 표시되는지 확인
5. 과거 날짜 선택 시 → Flatpickr가 선택 불가 처리하는지 확인 (`minDate: 'today'`)

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/reservation/reservation-modify.mustache
git commit -m "feat(reservation): disable booked time slots in reservation-modify with excludeId"
```

---

## 완료 체크리스트

- [ ] `GET /api/reservation/booked-slots` 정상 응답 확인
- [ ] 직접 예약 페이지: 예약된 슬롯 비활성화
- [ ] 직접 예약 페이지: 날짜 재선택 시 슬롯 목록 갱신
- [ ] 직접 예약 페이지: 의사 변경 시 슬롯 전체 초기화
- [ ] 변경 페이지: 내 예약 슬롯은 활성 상태 (excludeId 동작)
- [ ] 변경 페이지: `minDate: 'today'` 과거 날짜 선택 불가
- [ ] 전체 테스트 통과: `./gradlew test`
