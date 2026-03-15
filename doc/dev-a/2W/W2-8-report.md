# W2-#8 Report: @Valid 유효성 검증

## 작업 개요

예약 생성 폼(`POST /reservation/create`) 제출 시 서버 사이드 유효성 검증을 적용했다.
`ReservationCreateForm`에 Bean Validation 어노테이션을 추가하고,
컨트롤러에서 검증 실패 시 에러 메시지를 폼 화면으로 전달한다.

---

## 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `reservation/ReservationCreateForm.java` | `@NotBlank`, `@NotNull` 어노테이션 추가 |
| `reservation/ReservationController.java` | `@Valid` + `BindingResult` 적용, 검증 실패 시 폼 재표시 |
| `templates/reservation/direct-reservation.mustache` | 에러 메시지 배너 추가 |

---

## 구현 상세

### 1. ReservationCreateForm — Bean Validation 어노테이션 추가

```java
@Getter @Setter
public class ReservationCreateForm {
    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "연락처를 입력해주세요.")
    private String phone;

    @NotNull(message = "진료과를 선택해주세요.")
    private Long departmentId;

    @NotNull(message = "전문의를 선택해주세요.")
    private Long doctorId;

    @NotNull(message = "예약 날짜를 선택해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate reservationDate;

    @NotBlank(message = "예약 시간을 선택해주세요.")
    private String timeSlot;
}
```

---

### 2. ReservationController — @Valid + BindingResult 적용

```java
@PostMapping("/create")
public String createReservation(@Valid @ModelAttribute ReservationCreateForm form,
                                BindingResult bindingResult,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        String errorMessage = bindingResult.getAllErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining(" "));
        request.setAttribute("pageTitle", "직접 선택 예약");
        request.setAttribute("errorMessage", errorMessage);
        return "reservation/direct-reservation";
    }
    // ... 기존 로직 (PRG 패턴) ...
}
```

**설계 결정**: 검증 실패 시 PRG 리다이렉트 없이 폼 뷰를 직접 반환한다.
PRG 패턴은 성공 시 중복 제출 방지 목적이므로, 검증 실패 시에는 적용하지 않는 것이 표준이다.

---

### 3. direct-reservation.mustache — 에러 메시지 배너

```html
{{#errorMessage}}
<div class="flex items-center gap-2 p-4 bg-red-50 border border-red-200 rounded-xl text-sm text-red-600">
  <i data-feather="alert-circle" class="w-4 h-4 shrink-0"></i>
  <span>{{errorMessage}}</span>
</div>
{{/errorMessage}}
```

---

## 동작 흐름

```
[검증 성공]
POST /reservation/create (유효한 폼 데이터)
  └─ @Valid 통과
  └─ createReservation() 호출
  └─ redirect:/reservation/complete (PRG)

[검증 실패]
POST /reservation/create (빈 이름 또는 전화번호)
  └─ @Valid 실패 → BindingResult에 에러 수집
  └─ 에러 메시지 request attribute에 설정
  └─ reservation/direct-reservation 뷰 반환 (에러 배너 표시)
```

---

## 수용 기준 확인

- [x] 빈 이름 제출 시 "이름을 입력해주세요." 에러 메시지 표시
- [x] 빈 전화번호 제출 시 "연락처를 입력해주세요." 에러 메시지 표시
- [x] 진료과/전문의/날짜/시간 미선택 시 서버 사이드 검증 적용
- [x] 검증 성공 시 기존 PRG 패턴 동작 유지
