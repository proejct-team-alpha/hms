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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 폼 DTO의 각 필드에 유효성 검증 규칙을 추가합니다. `@NotBlank`는 빈 문자열/공백만 입력하면 오류, `@NotNull`은 null 값이면 오류를 발생시킵니다.
> - **왜 이렇게 썼는지**: Bean Validation(Java 표준 유효성 검증)을 사용합니다. `@NotBlank`는 문자열용(공백 체크), `@NotNull`은 객체/숫자용(null 체크)으로 구분합니다. `message` 속성으로 오류 메시지를 직접 지정합니다. `@DateTimeFormat`은 날짜 문자열을 `LocalDate`로 자동 변환할 포맷을 지정합니다.
> - **쉽게 말하면**: 예약 신청서의 필수 항목 칸에 "이 칸은 반드시 채워야 합니다"라는 규칙을 붙여두는 것입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 폼 데이터를 받을 때 `@Valid`로 유효성 검증을 수행하고, 오류가 있으면 오류 메시지를 모아 폼 화면으로 돌려보냅니다.
> - **왜 이렇게 썼는지**: `@Valid`는 "이 파라미터에 Bean Validation을 적용해라"는 표시입니다. `BindingResult`는 검증 결과를 담는 객체로, `hasErrors()`로 오류 여부를 확인합니다. `.stream().map(...).collect(Collectors.joining(" "))`은 여러 오류 메시지를 공백으로 이어 붙입니다. 검증 실패 시 리다이렉트 없이 뷰를 직접 반환하는 것이 표준입니다.
> - **쉽게 말하면**: 예약 신청서를 받아 빠진 항목이 있으면 "이 항목을 채워주세요"라는 오류 메시지와 함께 신청서를 돌려주는 창구입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `errorMessage`가 있을 때만 빨간색 오류 배너를 폼 화면에 표시합니다.
> - **왜 이렇게 썼는지**: Mustache의 `{{#errorMessage}}...{{/errorMessage}}`는 `errorMessage` 값이 있을 때만 내용을 렌더링합니다. `data-feather="alert-circle"`은 경고 아이콘, `bg-red-50 border border-red-200 text-red-600`은 Tailwind CSS로 빨간색 테마 배너를 만듭니다.
> - **쉽게 말하면**: 오류가 있을 때만 "빨간 경고 배너"가 나타나도록 조건부로 표시하는 것입니다.

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
