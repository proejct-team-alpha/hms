# @Valid 유효성 검증 전체 적용 설계 스펙

**작성일:** 2026-03-18
**브랜치:** feature/itemManager
**범위:** 전체 프로젝트 POST 엔드포인트 중 @Valid 미적용 영역

---

## 1. 배경 및 목표

### 현황
- `spring-boot-starter-validation` 의존성 이미 포함
- `GlobalExceptionHandler`에 `MethodArgumentNotValidException`, `ConstraintViolationException` 처리 완비
- Reservation·Staff 도메인: @Valid 완전 적용됨
- Item-Manager·Admin·Nurse·Doctor 도메인: @RequestParam 직접 수신 + 수동 검증 혼재

### 목표
- 모든 POST 엔드포인트에 DTO + `@Valid` 적용
- `parseQuantity()` 등 수동 검증 코드 제거
- 검증 로직을 DTO 레이어로 통일

---

## 2. 적용 범위

### 2.1 형식 분류

POST 엔드포인트는 두 가지 형식으로 구분한다.

**Form 제출 엔드포인트** (HTML form → redirect)
- 검증 실패: `BindingResult` → `redirectAttributes.addFlashAttribute("errorMessage", ...)` → redirect

**AJAX 엔드포인트** (`@ResponseBody` 반환)
- 검증 실패: `BindingResult` → `ResponseEntity.badRequest().body(Map.of("error", msg))`

### 2.2 대상 엔드포인트 목록

| 컨트롤러 | 메서드 | 엔드포인트 | 형식 | 신규 DTO |
|---------|--------|-----------|------|---------|
| ItemManagerController | saveItem | POST /item-manager/item-form/save | Form | `ItemSaveRequest` |
| ItemManagerController | restockItem | POST /item-manager/item/restock | Form | `ItemRestockRequest` |
| ItemManagerController | restockItemAjax | POST /item-manager/item/restock/ajax | AJAX | `ItemAmountRequest` |
| ItemManagerController | useItem | POST /item-manager/item-use | AJAX | `ItemAmountRequest` |
| AdminItemController | save | POST /admin/item/form/save | Form | `ItemSaveRequest` (공유) |
| AdminItemController | restock | POST /admin/item/restock | Form | `ItemRestockRequest` (공유) |
| AdminItemController | useItem | POST /admin/item/use | AJAX | `ItemAmountRequest` (공유) |
| AdminDepartmentController | create | POST /admin/department/form | Form | `DepartmentSaveRequest` |
| AdminRuleController | create | POST /admin/rule/form | Form | `RuleSaveRequest` |
| NurseReceptionController | updatePatient | POST /nurse/patient/update | Form | `PatientUpdateRequest` |
| NurseReceptionController | useItem | POST /nurse/item/use | AJAX | `ItemAmountRequest` (공유) |
| DoctorTreatmentController | completeTreatment | POST /doctor/treatment/complete | Form | `TreatmentCompleteRequest` |
| DoctorTreatmentController | useItem | POST /doctor/item/use | AJAX | `ItemAmountRequest` (공유) |
| StaffItemController | useItem | POST /staff/item/use | AJAX | `ItemAmountRequest` (공유) |
| StaffItemController | restockItem | POST /staff/item/restock | AJAX | `ItemAmountRequest` (공유) |

**적용 제외 (검증 불필요):**
- `deleteItem`, `receiveReservation`, `startTreatment` 등 단순 id 전달 메서드
  → id는 `@RequestParam Long id` 유지 (타입 자체가 검증 역할)

---

## 3. 신규 DTO 명세

### 3.1 `ItemSaveRequest`
**위치:** `com.smartclinic.hms.item.dto.ItemSaveRequest`
**공유:** ItemManagerController, AdminItemController

```java
public class ItemSaveRequest {
    private Long id;                         // null 허용 (신규 등록)

    @NotBlank(message = "물품명을 입력해주세요.")
    @Size(max = 100, message = "물품명은 100자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "카테고리를 선택해주세요.")
    private String category;

    @Min(value = 0, message = "수량은 0 이상이어야 합니다.")
    @Max(value = Integer.MAX_VALUE, message = "수량이 너무 큽니다.")
    private int quantity;

    @Min(value = 0, message = "최소 수량은 0 이상이어야 합니다.")
    private int minQuantity;
}
```

**참고:** 기존 `ItemFormDto`는 응답용(뷰 렌더링)으로 유지. `ItemSaveRequest`는 입력 전용.

### 3.2 `ItemRestockRequest`
**위치:** `com.smartclinic.hms.item.dto.ItemRestockRequest`

```java
public class ItemRestockRequest {
    @NotNull(message = "물품 ID가 필요합니다.")
    private Long id;

    @Min(value = 1, message = "입고 수량은 1 이상이어야 합니다.")
    private int amount;
}
```

**참고:**
- `redirectTo` 파라미터는 `ItemManagerController.restockItem`에만 존재하며, DTO 외부에서 별도 `@RequestParam(required = false)`으로 유지한다.
- `AdminItemController.restock`에는 `redirectTo`가 없으며, 에러 발생 시 redirect 경로는 `redirect:/admin/item/list`로 고정 유지한다.

### 3.3 `ItemAmountRequest`
**위치:** `com.smartclinic.hms.item.dto.ItemAmountRequest`
**공유:** 모든 AJAX useItem/restock 엔드포인트

```java
public class ItemAmountRequest {
    @NotNull(message = "물품 ID가 필요합니다.")
    private Long id;

    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private int amount;

    private Long reservationId;              // null 허용 (Nurse/Doctor용)
}
```

**엔드포인트별 reservationId 처리:**
- `NurseReceptionController.useItem`, `DoctorTreatmentController.useItem`: `req.getReservationId()` → 서비스에 전달
- `ItemManagerController.useItem`, `AdminItemController.useItem`, `StaffItemController.useItem`, `StaffItemController.restockItem`: `reservationId` 미사용, 서비스 호출 시 `null` 전달

**Authentication 파라미터:**
- `StaffItemController.useItem`은 `Authentication auth` 파라미터를 유지한다 (`getTodayUsageLogsByUser(auth.getName())` 호출용). DTO와 함께 메서드 파라미터로 공존시킨다.

### 3.4 `DepartmentSaveRequest`
**위치:** `com.smartclinic.hms.admin.department.dto.DepartmentSaveRequest`

```java
public class DepartmentSaveRequest {
    @NotBlank(message = "진료과 이름을 입력해주세요.")
    @Size(max = 100, message = "진료과 이름은 100자 이하여야 합니다.")
    private String name;
}
```

### 3.5 `RuleSaveRequest`
**위치:** `com.smartclinic.hms.admin.rule.dto.RuleSaveRequest`

```java
public class RuleSaveRequest {
    @NotBlank(message = "규정 제목을 입력해주세요.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "규정 내용을 입력해주세요.")
    private String content;

    @NotBlank(message = "카테고리를 선택해주세요.")
    private String category;
}
```

### 3.6 `PatientUpdateRequest`
**위치:** `com.smartclinic.hms.nurse.dto.PatientUpdateRequest`

```java
public class PatientUpdateRequest {
    @NotNull(message = "환자 ID가 필요합니다.")
    private Long patientId;

    @NotNull(message = "예약 ID가 필요합니다.")
    private Long reservationId;

    @NotBlank(message = "전화번호를 입력해주세요.")
    @Pattern(regexp = "\\d{2,3}-\\d{3,4}-\\d{4}",
             message = "올바른 전화번호 형식을 입력해주세요. (예: 010-1234-5678)")
    private String phone;

    private String address;                  // 선택 입력
    private String note;                     // 선택 입력
}
```

**에러 redirect 경로:**
- 검증 실패 시: `redirect:/nurse/patient-detail?id=` + `req.getReservationId()`

### 3.7 `TreatmentCompleteRequest`
**위치:** `com.smartclinic.hms.doctor.treatment.dto.TreatmentCompleteRequest`

```java
public class TreatmentCompleteRequest {
    @NotNull(message = "진료 ID가 필요합니다.")
    private Long id;

    @NotBlank(message = "진단명을 입력해주세요.")
    private String diagnosis;

    @NotBlank(message = "처방 내용을 입력해주세요.")
    private String prescription;

    private String remark;                   // 선택 입력
}
```

**Authentication 파라미터:**
- `DoctorTreatmentController.completeTreatment`는 `Authentication auth` 파라미터를 유지한다. DTO에 포함하지 않으며, `auth.getName()`을 서비스에 전달한다.

---

## 4. 컨트롤러 변경 패턴

### 4.1 Form 제출 엔드포인트 패턴

```java
// AS-IS
@PostMapping("/item-form/save")
public String saveItem(
        @RequestParam(name = "id", required = false) Long id,
        @RequestParam("name") String name,
        @RequestParam("category") String category,
        @RequestParam("quantity") String quantityStr,
        @RequestParam("minQuantity") String minQuantityStr,
        RedirectAttributes ra) {
    int quantity = parseQuantity(quantityStr, "수량");
    itemService.saveItem(id, name, category, quantity, minQuantity);
    ...
}

// TO-BE
@PostMapping("/item-form/save")
public String saveItem(
        @Valid @ModelAttribute ItemSaveRequest req,
        BindingResult br,
        RedirectAttributes ra) {
    if (br.hasErrors()) {
        ra.addFlashAttribute("errorMessage",
            br.getFieldErrors().get(0).getDefaultMessage());
        return req.getId() != null
            ? "redirect:/item-manager/item-form?id=" + req.getId()
            : "redirect:/item-manager/item-form";
    }
    itemService.saveItem(req.getId(), req.getName(), req.getCategory(),
                         req.getQuantity(), req.getMinQuantity());
    ...
}
```

### 4.2 AJAX 엔드포인트 패턴

```java
// AS-IS
@PostMapping("/item-use")
@ResponseBody
public ResponseEntity<?> useItem(
        @RequestParam Long id,
        @RequestParam String amountStr) {
    long parsed = Long.parseLong(amountStr.trim());
    if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
        return ResponseEntity.badRequest().body(Map.of("error", "수량은 1 이상이어야 합니다."));
    }
    int newQty = itemService.useItem(id, (int) parsed, null);
    ...
}

// TO-BE
@PostMapping("/item-use")
@ResponseBody
public ResponseEntity<?> useItem(
        @Valid @ModelAttribute ItemAmountRequest req,
        BindingResult br) {
    if (br.hasErrors()) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", br.getFieldErrors().get(0).getDefaultMessage()));
    }
    int newQty = itemService.useItem(req.getId(), req.getAmount(), req.getReservationId());
    ...
}
```

---

## 5. Service 계층 변경 없음

Service 메서드 시그니처는 **변경하지 않는다.**
컨트롤러에서 DTO 필드를 꺼내 기존 Service 메서드에 전달한다.

```java
// Service 시그니처 유지
itemService.saveItem(id, name, category, quantity, minQuantity);
itemService.restockItem(id, amount);
itemService.useItem(id, amount, reservationId);
```

---

## 6. 제거 대상 코드

각 컨트롤러에 중복 정의된 수동 검증 메서드를 제거한다.

- `ItemManagerController.parseQuantity()` — 삭제
- `AdminItemController.parseQuantity()` — 삭제
- AJAX 엔드포인트 내 `Long.parseLong()` + 수동 범위 검증 로직 — 삭제

---

## 7. 테스트 전략

### 7.1 기존 테스트 영향 없음
MockMvc `.param("name", "...")` 방식은 `@ModelAttribute` DTO에도 그대로 동작.
기존 테스트 코드 수정 불필요.

### 7.2 신규 검증 실패 테스트 추가
각 컨트롤러 테스트에 다음 시나리오 추가:
- 빈 값 → 에러 메시지 포함 redirect (Form 엔드포인트)
- 음수/0 수량 → 400 Bad Request (AJAX 엔드포인트)
- 필수 필드 누락 → 에러 처리 확인

### 7.3 신규 테스트 파일 생성 없음
범위를 검증 로직 추가로 한정. DoctorTreatmentControllerTest, StaffItemControllerTest 신규 생성은 별도 작업으로 분리.

---

## 8. 패키지 구조

```
com.smartclinic.hms
├── item/
│   └── dto/
│       ├── ItemSaveRequest.java       (신규)
│       ├── ItemRestockRequest.java    (신규)
│       ├── ItemAmountRequest.java     (신규)
│       ├── ItemFormDto.java           (기존 유지)
│       └── ItemListDto.java           (기존 유지)
├── admin/
│   ├── department/
│   │   └── dto/
│   │       └── DepartmentSaveRequest.java  (신규)
│   └── rule/
│       └── dto/
│           └── RuleSaveRequest.java        (신규)
├── nurse/
│   └── dto/
│       └── PatientUpdateRequest.java       (신규)
└── doctor/
    └── treatment/
        └── dto/
            └── TreatmentCompleteRequest.java (신규)
```

---

## 9. 구현 순서

1. 신규 DTO 7개 생성 (getter/setter 또는 record, Lombok @Getter)
2. ItemManagerController — saveItem, restockItem, restockItemAjax, useItem 수정
3. AdminItemController — save, restock, useItem 수정
4. AdminDepartmentController — create 수정
5. AdminRuleController — create 수정
6. NurseReceptionController — updatePatient, useItem 수정
7. DoctorTreatmentController — completeTreatment, useItem 수정
8. StaffItemController — useItem, restockItem 수정
9. `parseQuantity()` 메서드 제거
10. 전체 테스트 실행 확인
