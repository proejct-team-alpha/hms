# HMS 코드 리뷰 29건 수정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `doc/CODE_REVIEW_2026-03-25.md`에 기록된 29건 이슈(CRITICAL 3 / HIGH 12 / MEDIUM 9 / LOW 5)를 리스크 오름차순으로 전부 수정하고 테스트를 통과시킨다.

**Architecture:** 5개 그룹으로 순차 실행 — 비파괴(Group 1) → 구조 분리(Group 2) → 파라미터/로직(Group 3) → 아키텍처(Group 4) → 기타(Group 5). 각 그룹은 독립적으로 컴파일·테스트 가능하도록 설계한다.

**Tech Stack:** Spring Boot 3, Spring MVC (SSR + REST), Spring Security, Spring Data JPA, JUnit 5, Mockito/BDDMockito, @WebMvcTest

---

## File Map (생성/수정 대상 파일)

| 파일 | 작업 | 이슈 |
|------|------|------|
| `llm/service/MedicalService.java` | Modify (import 2줄) | C-01 |
| `admin/staff/AdminStaffService.java` | Modify (lines 55–68 문자열 상수) | C-03 |
| `static/js/pages/admin-rule-form.js` | Modify (lines 1–10) | C-02 |
| `static/js/pages/staff-reception-detail.js` | Modify (lines 1–11) | C-02 |
| `static/js/pages/doctor-treatment-detail.js` | Modify (lines 1–17) | C-02 |
| `static/js/pages/admin-dashboard.js` | Modify (lines 224–229) | C-02 / N-M-01 |
| `static/js/pages/admin-rule-form.js` | Modify (var→const) | M-06 |
| `static/js/pages/admin-staff-form.js` | Modify (var→const) | M-06 |
| `static/js/pages/staff-reception-detail.js` | Modify (var→const) | M-06 |
| `static/js/pages/doctor-treatment-detail.js` | Modify (var→const) | M-06 |
| `_sample/SampleReservation.java` 외 | Modify (@Profile("dev") 추가) | M-07 |
| `nurse/NurseService.java:86-97` | Modify (메서드 삭제) | M-08 |
| `static/css/style-admin.css` | Delete (중복 파일) | H-08 |
| `static/css/style-doctor.css` | Delete (중복 파일) | H-08 |
| `static/js/data-admin.js`, `data-staff.js`, `data-nurse.js` | Delete (dead code) | L-04 |
| `admin/reservation/AdminReservationRepository.java` | Modify (nativeQuery → JPQL) | L-05 |
| `admin/item/AdminItemController.java` | Modify (useItem 제거) | H-04 |
| `admin/item/AdminItemApiController.java` | **Create** (@RestController) | H-04 |
| `nurse/NurseReceptionController.java` | Modify (item 엔드포인트 제거) | H-09 |
| `nurse/NurseItemApiController.java` | **Create** (@RestController) | H-09 |
| `doctor/treatment/DoctorTreatmentController.java` | Modify (poll/item 엔드포인트 제거, Model→HttpServletRequest) | N-H-01, N-H-02 |
| `doctor/treatment/DoctorTreatmentApiController.java` | **Create** (@RestController) | N-H-01 |
| `staff/reception/ReceptionController.java` | Modify (AJAX 엔드포인트 제거, list/detail Model→HttpServletRequest, list 로직 위임) | N-H-03, N-H-04, N-H-05 |
| `staff/reception/ReceptionApiController.java` | **Create** (@RestController) | N-H-03 |
| `staff/reception/ReceptionService.java` | Modify (list 로직 흡수, N+1 H-06 수정, H-05 수정) | N-H-05, H-05, H-06 |
| `staff/reception/dto/ReceptionListResult.java` | **Create** (페이지 결과 DTO Record) | N-H-05 |
| `staff/walkin/WalkinController.java` | Modify (GET Model→HttpServletRequest) | N-H-04 |
| `staff/reservation/PhoneReservationController.java` | Modify (GET Model→HttpServletRequest) | N-H-04 |
| `nurse/NurseService.java` | Modify (N+1 H-07 수정) | H-07 |
| `doctor/treatment/DoctorTreatmentRecordRepository.java` | Modify (findAllByReservation_IdIn 추가) | H-07 |
| `admin/staff/AdminStaffController.java` | Modify (message contains → enum 에러 코드) | N-M-04 |
| `common/exception/StaffErrorCode.java` | **Create** (에러 코드 enum) | N-M-04 |
| `reservation/reservation/ReservationRepository.java` (및 admin, nurse 중복) | 정식 Repository로 메서드 병합, 중복 Repository 삭제 | H-01 |
| `nurse/NurseService.java`, `staff/reception/ReceptionService.java` | Modify (Clock 주입) | M-03 |
| `doctor/treatment/DoctorTreatmentService.java` | Modify (Clock 주입) | N-M-02 |
| `common/interceptor/InactiveStaffLogoutInterceptor.java` | Modify (세션 캐시) | N-M-03 |
| `test/common/SharedTestSecurityConfig.java` | **Create** (공통 TestSecurityConfig) | M-04 |
| 기존 10+ WebMvcTest 클래스 | Modify (내부 중복 제거) | M-04 |
| `test/llm/LlmReservationServiceTest.java` | Modify (테스트 보강) | M-05 |
| `test/llm/SymptomAnalysisServiceTest.java` | Modify (테스트 보강) | M-05 |
| `domain/Doctor.java` 외 | Modify (availableDays 정규화) | L-01 |
| `common/interceptor/LayoutModelInterceptor.java` | Modify (boolean 플래그 정리) | L-02 |
| `domain/StaffRole.java` | Modify (getDashboardUrl() 추가) | L-03 |

---

## Group 1: 비파괴 수정 (C-01, C-03, M-06, M-07, M-08, H-08, L-04, L-05)

### Task 1: C-01 — tools.jackson → com.fasterxml.jackson

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/llm/service/MedicalService.java:22-23`

- [ ] **Step 1: import 2줄 수정**

```java
// 변경 전 (line 22-23)
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

// 변경 후
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
```

- [ ] **Step 2: 컴파일 확인**

```bash
cd c:/workspace/gitstudy_lab/demo/team-demo/team-project-demo/hms
./mvnw compile -pl . -am -q 2>&1 | tail -5
```
Expected: BUILD SUCCESS (오류 없음)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/smartclinic/hms/llm/service/MedicalService.java
git commit -m "fix: C-01 tools.jackson → com.fasterxml.jackson import 수정"
```

---

### Task 2: C-03 — AdminStaffService 문자열 상수 인코딩 수정

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java:55-68`

- [ ] **Step 1: 깨진 상수 14개를 올바른 문자열로 교체**

line 55~68의 깨진 바이트열 상수를 아래 Unicode 이스케이프 + 의미에 맞는 한글 문자열로 교체한다.
(line 69 `INACTIVE_STAFF_UPDATE_NOT_ALLOWED_MESSAGE` 형식 참고 — `\uXXXX` 이스케이프 사용)

```java
private static final String STAFF_CREATED_MESSAGE = "\uC9C1\uC6D0\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
private static final String STAFF_UPDATED_MESSAGE = "\uC9C1\uC6D0 \uC815\uBCF4\uAC00 \uC218\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
private static final String STAFF_DEACTIVATED_MESSAGE = "\uC9C1\uC6D0\uC774 \uBE44\uD65C\uC131\uD654\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
private static final String INPUT_CHECK_MESSAGE = "\uC785\uB825\uAC12\uC744 \uD655\uC778\uD574 \uC8FC\uC138\uC694.";
private static final String INVALID_ROLE_MESSAGE = "\uC720\uD6A8\uD558\uC9C0 \uC54A\uC740 \uC5ED\uD560\uC785\uB2C8\uB2E4.";
private static final String INVALID_DEPARTMENT_MESSAGE = "\uC720\uD6A8\uD558\uC9C0 \uC54A\uC740 \uBD80\uC11C\uC785\uB2C8\uB2E4.";
private static final String DUPLICATE_USERNAME_MESSAGE = "\uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC778 \uC544\uC774\uB514\uC785\uB2C8\uB2E4.";
private static final String DUPLICATE_EMPLOYEE_NUMBER_MESSAGE = "\uC774\uBBF8 \uC0AC\uC6A9 \uC911\uC778 \uC0AC\uBC88\uC785\uB2C8\uB2E4.";
private static final String STAFF_NOT_FOUND_MESSAGE = "\uC9C1\uC6D0\uC744 \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
private static final String DOCTOR_NOT_FOUND_MESSAGE = "\uC758\uC0AC \uC815\uBCF4\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
private static final String PASSWORD_LENGTH_MESSAGE = "\uBE44\uBC00\uBC88\uD638\uB294 8\uC790 \uC774\uC0C1\uC774\uC5B4\uC57C \uD569\uB2C8\uB2E4.";
private static final String SELF_DEACTIVATE_MESSAGE = "\uC790\uAE30 \uC790\uC2E0\uC740 \uBE44\uD65C\uC131\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
private static final String ALREADY_DEACTIVATED_MESSAGE = "\uC774\uBBF8 \uBE44\uD65C\uC131\uD654\uB41C \uC9C1\uC6D0\uC785\uB2C8\uB2E4.";
private static final String REACTIVATION_NOT_ALLOWED_MESSAGE = "\uD574\uB2F9 \uC9C1\uC6D0\uC740 \uC7AC\uD65C\uC131\uD654\uD560 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.";
```

- [ ] **Step 2: 컴파일 확인**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```
Expected: BUILD SUCCESS

- [ ] **Step 3: 기존 AdminStaffServiceTest 실행**

```bash
./mvnw test -pl . -Dtest=AdminStaffServiceTest -q 2>&1 | tail -10
```
Expected: Tests run: N, Failures: 0, Errors: 0

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java
git commit -m "fix: C-03 AdminStaffService 깨진 문자열 상수 UTF-8 수정"
```

---

### Task 3: C-02 / N-M-01 — innerHTML XSS 제거 (4개 JS 파일)

**Files:**
- Modify: `src/main/resources/static/js/pages/admin-rule-form.js:1-11`
- Modify: `src/main/resources/static/js/pages/staff-reception-detail.js:1-11`
- Modify: `src/main/resources/static/js/pages/doctor-treatment-detail.js:1-17`
- Modify: `src/main/resources/static/js/pages/admin-dashboard.js:224-229`

**방침:** `innerHTML`에 고정 SVG/HTML 리터럴을 할당하는 패턴 → `lucide.createIcons()` API 또는 `createElement` + `setAttribute`/`textContent` 교체.

- [ ] **Step 1: admin-rule-form.js — lucide API 교체**

`container.innerHTML = '<i data-lucide="...">`를 다음 패턴으로 교체:

```js
function toggleActiveIcon() {
  const isActive = document.getElementById('ruleIsActive').checked;
  const container = document.getElementById('activeIconContainer');
  // 기존 아이콘 제거 후 새 아이콘 생성
  container.replaceChildren();
  const icon = document.createElement('i');
  icon.setAttribute('data-lucide', isActive ? 'check-circle-2' : 'x-circle');
  icon.className = isActive ? 'w-5 h-5 text-green-500' : 'w-5 h-5 text-slate-400';
  container.appendChild(icon);
  lucide.createIcons();
}
```

- [ ] **Step 2: staff-reception-detail.js — 스피너를 createElement 로 교체**

line 3의 `btn.innerHTML = '<div class="...">...</div> ...'`를:

```js
function handleReceive(event) {
  const btn = event.currentTarget;
  // 스피너 생성
  const spinner = document.createElement('div');
  spinner.className = 'w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin';
  const label = document.createTextNode(' 접수 완료 처리 (RECEIVED)');
  btn.replaceChildren(spinner, label);
  btn.disabled = true;
  btn.classList.add('opacity-50');
  // 이하 동일
```

- [ ] **Step 3: doctor-treatment-detail.js — 스피너를 createElement 로 교체**

line 9의 `btn.innerHTML = '<div ...>...</div> 진료 완료'`를:

```js
  const spinner = document.createElement('div');
  spinner.className = 'w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin';
  const label = document.createTextNode(' 진료 완료');
  btn.replaceChildren(spinner, label);
```

- [ ] **Step 4: admin-dashboard.js — innerHTML 빈 상태 렌더링 교체**

line 225: `container.innerHTML = renderItemFlowEmptyState();` →
`renderItemFlowEmptyState()`가 순수 정적 문자열(서버 데이터 미포함)인지 확인.
서버 데이터를 삽입하지 않는 정적 HTML이므로 `DOMParser` + `appendChild` 패턴으로 교체:

```js
if (!Array.isArray(itemFlowDays) || itemFlowDays.length === 0) {
  const parser = new DOMParser();
  const doc = parser.parseFromString(renderItemFlowEmptyState(), 'text/html');
  container.replaceChildren(...doc.body.childNodes);
  return;
}
// line 229 — template literal 차트 HTML 교체 (동일 패턴)
const chartHtml = `<div ...>...</div>`;
const parser = new DOMParser();
const doc = parser.parseFromString(chartHtml, 'text/html');
container.replaceChildren(...doc.body.childNodes);
```

> **주의:** `renderItemFlowColumn(item)` 함수가 외부 서버 데이터를 `textContent` 없이 문자열에 삽입하는 경우 별도 XSS 검토 필요. 현재 `item` 필드는 숫자형(count, date)으로 가정.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/pages/admin-rule-form.js \
        src/main/resources/static/js/pages/staff-reception-detail.js \
        src/main/resources/static/js/pages/doctor-treatment-detail.js \
        src/main/resources/static/js/pages/admin-dashboard.js
git commit -m "fix: C-02/N-M-01 innerHTML XSS 제거 — createElement + lucide API 교체"
```

---

### Task 4: M-06 — var → const/let (JS 파일 전체)

**Files:**
- Modify: `src/main/resources/static/js/pages/admin-rule-form.js`
- Modify: `src/main/resources/static/js/pages/admin-staff-form.js`
- Modify: `src/main/resources/static/js/pages/staff-reception-detail.js`
- Modify: `src/main/resources/static/js/pages/doctor-treatment-detail.js`

**규칙:** 재할당 없는 변수 → `const`, 재할당 있는 변수 → `let`. `var` 전부 제거.

- [ ] **Step 1: 각 파일 var → const/let 전환**

각 파일을 열어 `var` 선언을 `const` 또는 `let`으로 교체한다.
- 루프 카운터(`i`, `j`) → `let`
- 재할당(`=`)이 없는 변수 → `const`
- `if` 블록 내 조건에 따라 재할당되는 경우 → `let`

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/js/pages/admin-rule-form.js \
        src/main/resources/static/js/pages/admin-staff-form.js \
        src/main/resources/static/js/pages/staff-reception-detail.js \
        src/main/resources/static/js/pages/doctor-treatment-detail.js
git commit -m "fix: M-06 var → const/let 전환 (JS 파일)"
```

---

### Task 5: M-07 — _sample 패키지 @Profile("dev") 추가

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/_sample/SampleReservation.java`
- Modify: `src/main/java/com/smartclinic/hms/_sample/SampleBusinessException.java`
- Modify: `src/main/java/com/smartclinic/hms/_sample/dto/SampleReservationCreateRequest.java`
- Modify: `src/main/java/com/smartclinic/hms/_sample/dto/SampleReservationResponse.java`

`SampleReservation` 엔티티는 `@Entity` + `@Table`을 가지고 있어 DDL이 운영 환경에서 실행된다.
`@Profile("dev")`를 `@Entity` 클래스에 추가하거나, 해당 클래스를 `src/test/java`로 이동한다.

- [ ] **Step 1: SampleReservation에 @Profile 추가 (또는 test 소스셋으로 이동 확인)**

`@Entity` 클래스에 `@Profile("dev")`를 추가하는 방식 선택:

```java
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Entity
@Table(name = "sample_reservations")
public class SampleReservation { ... }
```

> 테스트 소스셋으로 이동하는 경우 `@Entity` 스캔 범위를 확인해야 하므로 `@Profile("dev")` 방식을 권장.

- [ ] **Step 2: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/smartclinic/hms/_sample/
git commit -m "fix: M-07 _sample 패키지 @Profile(dev) 추가"
```

---

### Task 6: M-08 — NurseService.toKoreanDayOfWeek() 미사용 메서드 제거

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/nurse/NurseService.java:86-97`

- [ ] **Step 1: private 메서드 toKoreanDayOfWeek(int) 삭제**

lines 83–97 (Javadoc 포함) 전체 제거.

- [ ] **Step 2: 컴파일 확인**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/smartclinic/hms/nurse/NurseService.java
git commit -m "fix: M-08 NurseService 미사용 toKoreanDayOfWeek() 제거"
```

---

### Task 7: H-08 — 중복 CSS 파일 삭제

**Files:**
- Delete: `src/main/resources/static/css/style-admin.css` (admin-style.css와 동일)
- Delete: `src/main/resources/static/css/style-doctor.css` (doctor-style.css와 동일)

- [ ] **Step 1: 두 CSS 파일이 실제로 동일한지 확인**

```bash
diff src/main/resources/static/css/admin-style.css src/main/resources/static/css/style-admin.css
diff src/main/resources/static/css/doctor-style.css src/main/resources/static/css/style-doctor.css
```
Expected: 두 diff 모두 출력 없음 (파일 내용 동일)

- [ ] **Step 2: Mustache 템플릿에서 style-admin.css / style-doctor.css 참조 검색**

```bash
grep -r "style-admin\|style-doctor" src/main/resources/templates/
```
만약 참조가 있으면 해당 템플릿의 `<link>` 경로를 `admin-style.css` / `doctor-style.css`로 교체.
참조가 없으면 바로 삭제.

- [ ] **Step 3: 파일 삭제**

```bash
git rm src/main/resources/static/css/style-admin.css
git rm src/main/resources/static/css/style-doctor.css
```

- [ ] **Step 4: Commit**

```bash
git commit -m "fix: H-08 중복 CSS 파일(style-admin/doctor) 삭제"
```

---

### Task 8: L-04 / L-05 — 죽은 JS 파일 삭제 + nativeQuery → JPQL

**Files:**
- Delete: `src/main/resources/static/js/data-admin.js`
- Delete: `src/main/resources/static/js/data-staff.js`
- Delete: `src/main/resources/static/js/data-nurse.js`
- Modify: `src/main/java/com/smartclinic/hms/admin/reservation/AdminReservationRepository.java`

- [ ] **Step 1: JS 파일 참조 검색**

```bash
grep -r "data-admin\.js\|data-staff\.js\|data-nurse\.js" src/main/resources/templates/
```
참조가 있으면 해당 `<script>` 태그 제거 후 삭제. 없으면 바로 삭제.

- [ ] **Step 2: JS 파일 삭제**

```bash
git rm src/main/resources/static/js/data-admin.js \
       src/main/resources/static/js/data-staff.js \
       src/main/resources/static/js/data-nurse.js 2>/dev/null || true
```

- [ ] **Step 3: AdminReservationRepository nativeQuery → JPQL**

`AdminReservationRepository.java`의 `nativeQuery = true` 쿼리를 JPQL로 전환한다.
(`@Query("SELECT r FROM Reservation r ...")` 방식, Spring Data 파생 메서드로 표현 가능한 경우 파생 메서드 우선)

- [ ] **Step 4: 컴파일 + AdminReservationServiceTest 실행**

```bash
./mvnw test -pl . -Dtest=AdminReservationServiceTest -q 2>&1 | tail -10
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: L-04 죽은 JS 파일 삭제, L-05 AdminReservationRepository nativeQuery → JPQL"
```

---

## Group 2: 구조 분리 — SSR/REST 혼재 컨트롤러 분리 (H-04, H-09, N-H-01, N-H-03, C-02 추가 대응)

**원칙:** `@Controller` 클래스에서 `@ResponseBody` 엔드포인트를 추출해 새 `@RestController`로 이동. 기존 URL 유지. `Resp.ok()` 사용.

---

### Task 9: H-04 — AdminItemController → AdminItemApiController 분리

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/admin/item/AdminItemController.java` (useItem 제거)
- Create: `src/main/java/com/smartclinic/hms/admin/item/AdminItemApiController.java`

- [ ] **Step 1: AdminItemApiController 생성**

```java
package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/item")
public class AdminItemApiController {

    private final ItemManagerService itemManagerService;

    @PostMapping("/use")
    public ResponseEntity<Resp<Map<String, Object>>> useItem(
            @RequestParam("id") Long id,
            @RequestParam("amount") String amountStr) {
        long parsed = Long.parseLong(amountStr.trim());
        if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
            return Resp.fail(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "올바른 수량을 입력해 주세요.");
        }
        int newQuantity = itemManagerService.useItem(id, (int) parsed, null);
        List<ItemUsageLogDto> logs = itemManagerService.getTodayStaffUsageLogs();
        long totalUsedAmount = itemManagerService.getTodayTotalStaffUsageAmount();
        Map<String, Object> body = Map.of(
                "quantity", newQuantity,
                "logs", logs,
                "todayLogCount", logs.size(),
                "todayTotalUsedAmount", totalUsedAmount);
        return Resp.ok(body);
    }
}
```

> 예외 처리는 `GlobalExceptionHandler`가 담당하므로 try-catch 불필요.

- [ ] **Step 2: AdminItemController에서 useItem 메서드 제거**

`AdminItemController.java:108-131`의 `@PostMapping("/use") @ResponseBody public ResponseEntity<?> useItem(...)` 메서드 삭제.
관련 import (`ResponseBody`, `ResponseEntity`, `Map`) 중 더 이상 사용되지 않는 것 제거.

- [ ] **Step 3: 컴파일 확인**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 4: AdminItemControllerTest + AdminItemServiceTest 실행**

```bash
./mvnw test -pl . -Dtest="AdminItemControllerTest,AdminItemServiceTest" -q 2>&1 | tail -10
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/smartclinic/hms/admin/item/
git commit -m "fix: H-04 AdminItemController useItem → AdminItemApiController(@RestController) 분리"
```

---

### Task 10: H-09 — NurseReceptionController → NurseItemApiController 분리

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/nurse/NurseReceptionController.java` (lines 382-415 제거)
- Create: `src/main/java/com/smartclinic/hms/nurse/NurseItemApiController.java`

- [ ] **Step 1: NurseItemApiController 생성**

```java
package com.smartclinic.hms.nurse;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseItemApiController {

    private final ItemManagerService itemManagerService;

    @PostMapping("/item/use")
    public ResponseEntity<Resp<Map<String, Object>>> useItem(
            @RequestParam("id") Long id,
            @RequestParam("amount") String amountStr,
            @RequestParam(name = "reservationId", required = false) Long reservationId) {
        long parsed = Long.parseLong(amountStr.trim());
        if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
            return Resp.fail(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "올바른 수량을 입력해주세요.");
        }
        int newQuantity = itemManagerService.useItem(id, (int) parsed, reservationId);
        List<ItemUsageLogDto> logs = reservationId != null
                ? itemManagerService.getUsageLogs(reservationId)
                : List.of();
        return Resp.ok(Map.of("quantity", newQuantity, "logs", logs));
    }

    @PostMapping("/item/cancel")
    public ResponseEntity<Resp<Map<String, Object>>> cancelUsage(
            @RequestParam("logId") Long logId,
            @RequestParam("reservationId") Long reservationId) {
        itemManagerService.cancelItemUsage(logId);
        List<ItemUsageLogDto> logs = itemManagerService.getUsageLogs(reservationId);
        return Resp.ok(Map.of("success", true, "logs", logs));
    }
}
```

- [ ] **Step 2: NurseReceptionController에서 item 엔드포인트 제거**

`NurseReceptionController.java:382-415` (`useItem`, `cancelUsage` 메서드) 삭제.
더 이상 필요 없는 import 정리 (`ResponseBody`, `ItemManagerService`, `ItemUsageLogDto` 등).

- [ ] **Step 3: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/smartclinic/hms/nurse/
git commit -m "fix: H-09 NurseReceptionController item 엔드포인트 → NurseItemApiController 분리"
```

---

### Task 11: N-H-01 — DoctorTreatmentController → DoctorTreatmentApiController 분리

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentController.java` (poll/item 엔드포인트 제거)
- Create: `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentApiController.java`

- [ ] **Step 1: DoctorTreatmentApiController 생성**

poll 엔드포인트(line 36-42)와 item 엔드포인트(lines 101-138) 이동.
기존 `ResponseEntity<?>` + `Map.of()` 반환을 `Resp.ok()` 방식으로 통일.

```java
package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorTreatmentApiController {

    private final DoctorTreatmentService treatmentService;
    private final ItemManagerService itemManagerService;

    @GetMapping("/treatment-list/poll")
    public ResponseEntity<Resp<List<DoctorReservationDto>>> pollTreatmentList(
            Authentication auth,
            @RequestParam(name = "query", required = false) String query) {
        List<DoctorReservationDto> list = treatmentService.getTodayReceivedList(auth.getName(), query);
        return Resp.ok(list);
    }

    @PostMapping("/item/use")
    public ResponseEntity<Resp<Map<String, Object>>> useItem(
            @RequestParam("id") Long id,
            @RequestParam("amount") String amountStr,
            @RequestParam(name = "reservationId", required = false) Long reservationId) {
        long parsed = Long.parseLong(amountStr.trim());
        if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
            return Resp.fail(org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "올바른 수량을 입력해주세요.");
        }
        int newQuantity = itemManagerService.useItem(id, (int) parsed, reservationId);
        List<ItemUsageLogDto> logs = reservationId != null
                ? itemManagerService.getUsageLogs(reservationId)
                : List.of();
        return Resp.ok(Map.of("quantity", newQuantity, "logs", logs));
    }

    @PostMapping("/item/cancel")
    public ResponseEntity<Resp<Map<String, Object>>> cancelItem(
            @RequestParam("logId") Long logId,
            @RequestParam("reservationId") Long reservationId) {
        Map<String, Object> result = itemManagerService.cancelItemUsage(logId);
        List<ItemUsageLogDto> logs = itemManagerService.getUsageLogs(reservationId);
        return Resp.ok(Map.of(
                "itemId", result.get("itemId"),
                "quantity", result.get("quantity"),
                "logs", logs));
    }
}
```

- [ ] **Step 2: DoctorTreatmentController에서 poll/item 엔드포인트 제거**

lines 35-42 (poll), lines 101-138 (useItem, cancelItem) 삭제.
관련 import 정리.

- [ ] **Step 3: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/smartclinic/hms/doctor/treatment/
git commit -m "fix: N-H-01 DoctorTreatmentController REST 엔드포인트 → DoctorTreatmentApiController 분리"
```

---

### Task 12: N-H-03 — ReceptionController AJAX → ReceptionApiController 분리

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/staff/reception/ReceptionController.java` (lines 273-337 제거)
- Create: `src/main/java/com/smartclinic/hms/staff/reception/ReceptionApiController.java`

- [ ] **Step 1: ReceptionApiController 생성**

`receiveAjax`, `cancelAjax`, `payAjax`, `updatePatientInfo` 4개 엔드포인트를 새 `@RestController`로 이동.
`Map<String,Object>` 반환을 `Resp.ok()` 방식으로 통일.

```java
package com.smartclinic.hms.staff.reception;

import com.smartclinic.hms.common.util.Resp;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.staff.reception.dto.PatientInfoUpdateRequest;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/staff/reception")
public class ReceptionApiController {

    private final ReceptionService receptionService;

    @PostMapping("/receive-ajax")
    public ResponseEntity<Resp<Map<String, Object>>> receiveAjax(@RequestBody ReceptionUpdateRequest request) {
        receptionService.receive(request);
        return Resp.ok(Map.of(
                "success", true,
                "statusText", "진료 대기",
                "statusClass", "bg-orange-100 text-orange-700"));
    }

    @PostMapping("/cancel-ajax")
    public ResponseEntity<Resp<Map<String, Object>>> cancelAjax(@RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        String reason = (String) request.getOrDefault("reason", "");
        Reservation r = receptionService.cancel(id, reason);
        String statusText = r.getStatus() == ReservationStatus.CANCELLED ? "취소" : "예약";
        String statusClass = r.getStatus() == ReservationStatus.CANCELLED
                ? "bg-red-100 text-red-700"
                : "bg-blue-100 text-blue-700";
        return Resp.ok(Map.of(
                "success", true,
                "newStatus", r.getStatus().name(),
                "statusText", statusText,
                "statusClass", statusClass));
    }

    @PostMapping("/pay-ajax")
    public ResponseEntity<Resp<Map<String, Object>>> payAjax(@RequestBody Map<String, Object> request) {
        Long id = Long.valueOf(request.get("id").toString());
        receptionService.completePayment(id);
        return Resp.ok(Map.of("success", true, "message", "수납이 완료되었습니다."));
    }

    @PostMapping("/update-patient-info")
    public ResponseEntity<Resp<Map<String, Object>>> updatePatientInfo(
            @RequestBody @Valid PatientInfoUpdateRequest request) {
        receptionService.updatePatientInfo(request);
        return Resp.ok(Map.of("success", true, "message", "환자 정보가 수정되었습니다."));
    }
}
```

- [ ] **Step 2: ReceptionController에서 4개 AJAX 엔드포인트 제거 (lines 273-337)**

관련 import 정리 (`ResponseBody`, `RequestBody`, `Map`, `HashMap` 등).

- [ ] **Step 3: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/smartclinic/hms/staff/reception/
git commit -m "fix: N-H-03 ReceptionController AJAX 엔드포인트 → ReceptionApiController 분리 + Resp.ok()"
```

---

## Group 3: 파라미터/로직 수정 (N-H-02, N-H-04, N-H-05, H-05, H-06, H-07, N-M-04)

---

### Task 13: N-H-02 + N-H-04 — GET SSR 핸들러 Model → HttpServletRequest

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentController.java`
- Modify: `src/main/java/com/smartclinic/hms/staff/reception/ReceptionController.java` (list, detail)
- Modify: `src/main/java/com/smartclinic/hms/staff/walkin/WalkinController.java`
- Modify: `src/main/java/com/smartclinic/hms/staff/reservation/PhoneReservationController.java`

**패턴:**
```java
// 변경 전
public String someView(Model model) {
    model.addAttribute("key", value);
    return "view-name";
}

// 변경 후
public String someView(HttpServletRequest request) {
    request.setAttribute("key", value);
    return "view-name";
}
```

- [ ] **Step 1: DoctorTreatmentController GET 핸들러 수정**

`treatmentList()`, `treatmentDetail()`, `completedDetail()` 세 핸들러의 `Model model` → `HttpServletRequest request` 교체.
`model.addAttribute(k, v)` → `request.setAttribute(k, v)`.
import: `org.springframework.ui.Model` 제거, `jakarta.servlet.http.HttpServletRequest` 추가.

- [ ] **Step 2: ReceptionController list() + detail() GET 핸들러 수정**

`list()` (line 41)와 `detail()` (line 224)의 `Model model` → `HttpServletRequest request`.
`model.addAttribute(k, v)` → `request.setAttribute(k, v)`.

> N-H-05 작업(list 로직 위임)과 연계됨. 이 단계에서는 시그니처만 교체하고 로직은 다음 Task에서 처리.

- [ ] **Step 3: WalkinController walkinPage() 수정**

`walkinPage()` (line 33)의 `Model model` → `HttpServletRequest request`.

- [ ] **Step 4: PhoneReservationController phoneReservation() 수정**

`phoneReservation()` (line 31)의 `Model model` → `HttpServletRequest request`.
(POST 핸들러 `createPhoneReservation()`은 PRG 패턴이므로 `Model model` 유지)

- [ ] **Step 5: 컴파일 확인**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentController.java \
        src/main/java/com/smartclinic/hms/staff/reception/ReceptionController.java \
        src/main/java/com/smartclinic/hms/staff/walkin/WalkinController.java \
        src/main/java/com/smartclinic/hms/staff/reservation/PhoneReservationController.java
git commit -m "fix: N-H-02/N-H-04 GET SSR 핸들러 Model → HttpServletRequest 교체"
```

---

### Task 14: N-H-05 + H-05 + H-06 — ReceptionService 로직 위임 + N+1 수정

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/staff/reception/ReceptionService.java`
- Modify: `src/main/java/com/smartclinic/hms/staff/reception/ReceptionController.java` (list 로직 제거)
- Create: `src/main/java/com/smartclinic/hms/staff/reception/dto/ReceptionListResult.java`
- Modify: `src/main/java/com/smartclinic/hms/reservation/reservation/ReservationRepository.java` (배치 쿼리 추가)

**N-H-05:** `ReceptionController.list()`의 200줄 로직(탭 필터, 수동 페이징, 파라미터 빌더)을 Service로 위임.
**H-05:** `createPhoneReservation()`의 전체 예약 로드 → `existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot()` 단일 쿼리.
**H-06:** `getReservations()` 스트림 내 `countByPatient_IdAndStatus()` N+1 → `countCompletedByPatientIds()` 배치 쿼리.

- [ ] **Step 1: ReceptionListResult Record 생성**

```java
package com.smartclinic.hms.staff.reception.dto;

import com.smartclinic.hms.staff.dto.StaffReservationDto;
import java.util.List;
import java.util.Map;

public record ReceptionListResult(
    List<StaffReservationDto> reservations,
    int currentPage,
    int totalPages,
    int totalCount,
    boolean hasPrev,
    boolean hasNext,
    int prevPage,
    int nextPage,
    List<Map<String, Object>> pageLinks
) {}
```

- [ ] **Step 2: ReservationRepository에 배치 쿼리 추가 (H-06)**

```java
// 환자 ID 목록에 대한 COMPLETED 건수를 한 번에 조회 (N+1 방지)
@Query("SELECT r.patient.id, COUNT(r) FROM Reservation r " +
       "WHERE r.patient.id IN :patientIds AND r.status = :status " +
       "GROUP BY r.patient.id")
List<Object[]> countCompletedByPatientIds(
    @Param("patientIds") List<Long> patientIds,
    @Param("status") ReservationStatus status);
```

- [ ] **Step 3: H-05 수정 — createPhoneReservation() 중복 체크**

`ReceptionService.java:92-98`의 전체 예약 로드 + 스트림 필터를 단일 쿼리로 교체:

```java
// 변경 전 (lines 92-98)
List<Reservation> existingReservations = reservationRepository.findTodayExcludingStatus(reservationDate, ReservationStatus.CANCELLED);
boolean isDuplicate = existingReservations.stream()
    .anyMatch(r -> r.getDoctor().getId().equals(doctor.getId()) && r.getTimeSlot().equals(request.getTime()));

// 변경 후
boolean isDuplicate = reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
    doctor.getId(), reservationDate, request.getTime(), ReservationStatus.CANCELLED);
```

- [ ] **Step 4: H-06 수정 — getReservations() N+1 제거**

`ReceptionService.java:216-221` 수정:

```java
// 변경 전 (스트림 내 N+1)
.map(r -> {
    long completedCount = reservationRepository.countByPatient_IdAndStatus(r.getPatient().getId(), ReservationStatus.COMPLETED);
    return new StaffReservationDto(r, completedCount);
})

// 변경 후 — 배치 조회 후 Map으로 활용
List<Long> patientIds = reservations.stream().map(r -> r.getPatient().getId()).distinct().collect(Collectors.toList());
Map<Long, Long> completedCountMap = reservationRepository
    .countCompletedByPatientIds(patientIds, ReservationStatus.COMPLETED)
    .stream()
    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

return reservations.stream()
    .filter(/* 기존 필터 동일 */)
    .map(r -> {
        long completedCount = completedCountMap.getOrDefault(r.getPatient().getId(), 0L);
        return new StaffReservationDto(r, completedCount);
    })
    .collect(Collectors.toList());
```

- [ ] **Step 5: N-H-05 수정 — list() 로직 Service로 위임**

`ReceptionService`에 다음 시그니처의 메서드 추가:

```java
public ReceptionListResult getReceptionListResult(
        LocalDate date, String tab, String query,
        List<Long> deptIds, List<Long> doctorIds, String source, int page) {
    // 1. dbStatus 매핑 (tab → RESERVED/RECEIVED/CANCELLED/null)
    // 2. getReservations() 호출
    // 3. treatment_status / paid 탭 정밀 필터링 + statusText 변환
    // 4. rowNum 부여
    // 5. 페이징 (pageSize=10, subList 기반 → 이미 Service에 있으므로 이동)
    // 6. pageLinks 목록 생성
    // 7. ReceptionListResult record 반환
}
```

`ReceptionController.list()`는 이 메서드를 호출한 뒤 `request.setAttribute`로만 구성:

```java
@GetMapping("/list")
public String list(HttpServletRequest request, /* @RequestParam들 */) {
    ReceptionListResult result = receptionService.getReceptionListResult(
        selectedDate, currentTab, query, deptIds, doctorIds, source, page);
    request.setAttribute("reservations", result.reservations());
    request.setAttribute("currentPage", result.currentPage());
    // ... result 필드를 setAttribute로 전달
    return "staff/reception-list";
}
```

- [ ] **Step 6: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/smartclinic/hms/staff/reception/ \
        src/main/java/com/smartclinic/hms/reservation/reservation/ReservationRepository.java
git commit -m "fix: N-H-05/H-05/H-06 ReceptionService 로직 위임 + N+1 제거 + 중복 체크 단일 쿼리"
```

---

### Task 15: H-07 — NurseService.getPatientDetail() N+1 수정

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/nurse/NurseService.java:205-224`
- Modify: `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentRecordRepository.java`

- [ ] **Step 1: DoctorTreatmentRecordRepository에 배치 조회 메서드 추가**

```java
List<TreatmentRecord> findAllByReservation_IdIn(List<Long> reservationIds);
```

- [ ] **Step 2: NurseService.getPatientDetail() N+1 수정**

lines 208-224의 루프 내 `treatmentRecordRepository.findByReservation_Id(h.getId())` 호출을 배치로 교체:

```java
// 변경 전 — 루프마다 DB 쿼리
.map(h -> {
    TreatmentRecord tr = treatmentRecordRepository.findByReservation_Id(h.getId()).orElse(null);
    ...
})

// 변경 후 — 배치 조회 후 Map 활용
List<Long> historyIds = histories.stream()
    .filter(h -> !h.getId().equals(reservationId) && h.getStatus() == ReservationStatus.COMPLETED)
    .map(Reservation::getId)
    .collect(Collectors.toList());

Map<Long, TreatmentRecord> trMap = treatmentRecordRepository
    .findAllByReservation_IdIn(historyIds)
    .stream()
    .collect(Collectors.toMap(tr -> tr.getReservation().getId(), tr -> tr));

List<NursePatientDto.NurseTreatmentHistoryDto> historyDtos = histories.stream()
    .filter(h -> !h.getId().equals(reservationId) && h.getStatus() == ReservationStatus.COMPLETED)
    .map(h -> {
        TreatmentRecord tr = trMap.get(h.getId());
        return new NursePatientDto.NurseTreatmentHistoryDto(...);
    })
    .toList();
```

- [ ] **Step 3: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/smartclinic/hms/nurse/NurseService.java \
        src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentRecordRepository.java
git commit -m "fix: H-07 NurseService.getPatientDetail() N+1 → findAllByReservation_IdIn 배치 조회"
```

---

### Task 16: N-M-04 — AdminStaffController 에러 라우팅 → enum 에러 코드 기반

**Files:**
- Create: `src/main/java/com/smartclinic/hms/admin/staff/StaffErrorCode.java`
- Modify: `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffController.java:138-183`
- Modify: `src/main/java/com/smartclinic/hms/admin/staff/AdminStaffService.java` (예외 throw 시 에러 코드 포함)

- [ ] **Step 1: StaffErrorCode enum 생성**

```java
package com.smartclinic.hms.admin.staff;

public enum StaffErrorCode {
    INVALID_DEPARTMENT,
    INVALID_ROLE,
    DUPLICATE_USERNAME,
    DUPLICATE_EMPLOYEE_NUMBER,
    STAFF_NOT_FOUND,
    DOCTOR_NOT_FOUND,
    PASSWORD_TOO_SHORT,
    SELF_DEACTIVATE,
    ALREADY_DEACTIVATED,
    REACTIVATION_NOT_ALLOWED,
    INACTIVE_STAFF_UPDATE
}
```

- [ ] **Step 2: AdminStaffService에서 CustomException throw 시 에러 코드 사용**

`CustomException`은 이미 `String errorCode` 필드와 `badRequest(String errorCode, String message)` 팩토리 메서드를 갖추고 있다(`common/exception/CustomException.java:54, 82`). 별도 수정 불필요.

```java
// 변경 전 (errorCode 없이 throw하는 경우)
throw CustomException.badRequest("VALIDATION_ERROR", "부서 정보를 찾을 수 없습니다.");

// 변경 후 (StaffErrorCode enum 사용)
throw CustomException.badRequest(StaffErrorCode.INVALID_DEPARTMENT.name(), "부서 정보를 찾을 수 없습니다.");
```

- [ ] **Step 3: AdminStaffController.applyCreateViewErrors() 수정**

```java
// 변경 전 — 문자열 포함 여부로 분기
if (e.getMessage().contains("부서")) { ... }

// 변경 후 — 에러 코드 기반 분기
String code = e.getErrorCode(); // CustomException에서 제공
if (StaffErrorCode.INVALID_DEPARTMENT.name().equals(code)) { ... }
```

- [ ] **Step 4: 컴파일 + AdminStaffControllerTest 실행**

```bash
./mvnw test -pl . -Dtest="AdminStaffControllerTest,AdminStaffServiceTest" -q 2>&1 | tail -10
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/smartclinic/hms/admin/staff/
git commit -m "fix: N-M-04 AdminStaffController 에러 라우팅 → StaffErrorCode enum 기반으로 교체"
```

---

## Group 4: 아키텍처 — 다중 Repository 통합 (H-01)

**대상:** `Reservation` 4개, `Patient` 3개, `Staff` 2개, `Department` 2개 Repository 중복.
**방침:** 각 엔티티의 정식(canonical) Repository 1개를 선정하고, 나머지 중복 Repository의 메서드를 정식 Repository로 병합. 중복 Repository를 삭제하고 이를 사용하던 Service/Controller의 import를 정식 Repository로 교체.

---

### Task 17: H-01 — Reservation 중복 Repository 통합

**정식 Repository:** `reservation/reservation/ReservationRepository.java`
**중복:** `admin/reservation/AdminReservationRepository.java` (Reservation 관련 메서드), `nurse/NursePatientStatusRepository.java` (Reservation 쿼리), `staff/` 내 Reservation 쿼리

- [ ] **Step 1: 정식 Repository 현황 파악**

```bash
grep -r "extends JpaRepository" src/main/java/com/smartclinic/hms/ | grep -i reservation
grep -r "extends JpaRepository" src/main/java/com/smartclinic/hms/ | grep -i patient
grep -r "extends JpaRepository" src/main/java/com/smartclinic/hms/ | grep -i staff
grep -r "extends JpaRepository" src/main/java/com/smartclinic/hms/ | grep -i department
```

- [ ] **Step 2: 정식 Repository에 누락 메서드 병합**

각 중복 Repository에서 정식 Repository에 없는 쿼리 메서드를 식별하고 복사.
메서드명 충돌 시 더 명확한 이름으로 통일.

- [ ] **Step 3: 중복 Repository를 참조하는 Service/Controller 업데이트**

```bash
# 사용 위치 확인
grep -r "AdminReservationRepository\|NursePatientRepository\|AdminPatientRepository" \
    src/main/java/ --include="*.java" -l
```
각 파일에서 중복 Repository import를 정식 Repository import로 교체.
생성자 주입 필드도 교체.

- [ ] **Step 4: 중복 Repository 파일 삭제**

삭제 전 해당 파일이 더 이상 참조되지 않음을 `grep`으로 확인.

- [ ] **Step 5: 컴파일 + 전체 테스트**

```bash
./mvnw test -pl . -q 2>&1 | tail -20
```
Expected: BUILD SUCCESS, 0 failures

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: H-01 동일 엔티티 다중 Repository 통합 — 정식 Repository 단일화"
```

---

## Group 5: 기타 — Clock, TestSecurityConfig, LLM 테스트, 인터셉터, L-01~L-03

---

### Task 18: M-03 + N-M-02 — LocalDate.now() → Clock 주입

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/nurse/NurseService.java:37`
- Modify: `src/main/java/com/smartclinic/hms/staff/reception/ReceptionService.java:178,303`
- Modify: `src/main/java/com/smartclinic/hms/doctor/treatment/DoctorTreatmentService.java:34,106,118,167,183,228`

**패턴:**
```java
// 변경 전
LocalDate today = LocalDate.now();

// 변경 후 (Clock 주입 후)
LocalDate today = LocalDate.now(clock);
```

```java
// Service 클래스
@Service
public class SomeService {
    private final Clock clock;

    public SomeService(/* 기존 의존성 */, Clock clock) { ... }
}
```

`Clock` 빈은 `@Configuration` 클래스에 등록:
```java
@Bean
public Clock clock() {
    return Clock.systemDefaultZone();
}
```

- [ ] **Step 1: ClockConfig 빈 등록 확인 (없으면 생성)**

```bash
grep -r "Clock" src/main/java/com/smartclinic/hms/config/ --include="*.java"
```
없으면 기존 Config 클래스에 `@Bean public Clock clock() { return Clock.systemDefaultZone(); }` 추가.

- [ ] **Step 2: NurseService — Clock 주입 + LocalDate.now() 교체**

생성자에 `Clock clock` 추가. `LocalDate.now()` → `LocalDate.now(clock)`.

- [ ] **Step 3: ReceptionService — Clock 주입 + LocalDate.now() 교체**

lines 178, 303의 `LocalDate.now()` → `LocalDate.now(clock)`.

- [ ] **Step 4: DoctorTreatmentService — Clock 주입 + LocalDate.now() 교체**

lines 34, 106, 118, 167, 183, 228의 `LocalDate.now()` / `LocalDateTime.now()` → `Clock` 기반으로 교체.

- [ ] **Step 5: 컴파일 + 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "fix: M-03/N-M-02 LocalDate.now() → Clock 주입으로 테스트 가능성 개선"
```

---

### Task 19: M-04 — TestSecurityConfig 중복 제거

**Files:**
- Create: `src/test/java/com/smartclinic/hms/common/SharedTestSecurityConfig.java`
- Modify: 기존 10+ WebMvcTest 클래스에서 내부 중복 `TestSecurityConfig` 제거

- [ ] **Step 1: 기존 공통 설정 파일 확인**

`AdminControllerTestSecurityConfig.java`와 `LlmWebMvcTestSecurityConfig.java`가 이미 공통 클래스로 존재.
각 WebMvcTest 클래스가 어떤 내부 클래스를 갖고 있는지 확인:

```bash
grep -r "TestConfiguration" src/test/ --include="*.java" -l
```

- [ ] **Step 2: SharedTestSecurityConfig 생성 (role 통합)**

```java
package com.smartclinic.hms.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
public class SharedTestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password("{noop}password").roles("ADMIN").build(),
                User.withUsername("doctor").password("{noop}password").roles("DOCTOR").build(),
                User.withUsername("nurse").password("{noop}password").roles("NURSE").build(),
                User.withUsername("staff").password("{noop}password").roles("STAFF").build());
    }
}
```

- [ ] **Step 3: 각 WebMvcTest 클래스에서 내부 중복 TestSecurityConfig 제거**

각 테스트 클래스의 `@Import` 어노테이션을 `SharedTestSecurityConfig.class`로 교체하거나 추가.
내부 `@TestConfiguration` 클래스 삭제.

- [ ] **Step 4: 전체 테스트 실행**

```bash
./mvnw test -pl . -q 2>&1 | tail -20
```
Expected: BUILD SUCCESS, 0 failures

- [ ] **Step 5: Commit**

```bash
git add src/test/
git commit -m "fix: M-04 TestSecurityConfig 중복 → SharedTestSecurityConfig 공통화"
```

---

### Task 20: M-05 — LLM 서비스 테스트 보강

**Files:**
- Modify: `src/test/java/com/smartclinic/hms/llm/LlmReservationServiceTest.java`
- Modify: `src/test/java/com/smartclinic/hms/llm/SymptomAnalysisServiceTest.java`

- [ ] **Step 1: 기존 테스트 파일 현황 파악**

```bash
cat src/test/java/com/smartclinic/hms/llm/LlmReservationServiceTest.java | head -50
cat src/test/java/com/smartclinic/hms/llm/SymptomAnalysisServiceTest.java | head -50
```

- [ ] **Step 2: LlmReservationServiceTest 핵심 케이스 추가**

BDDMockito + Given-When-Then + @DisplayName 패턴으로:
- 정상 예약 생성 흐름
- 슬롯 중복 시 예외 발생
- LLM 응답 파싱 실패 시 예외 발생

- [ ] **Step 3: SymptomAnalysisServiceTest 핵심 케이스 추가**

- 증상 분석 정상 흐름
- LLM 타임아웃 시 `LlmTimeoutException` 발생
- 서비스 불가 시 `LlmServiceUnavailableException` 발생

- [ ] **Step 4: 테스트 실행**

```bash
./mvnw test -pl . -Dtest="LlmReservationServiceTest,SymptomAnalysisServiceTest" -q 2>&1 | tail -10
```

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/smartclinic/hms/llm/
git commit -m "fix: M-05 LlmReservationService/SymptomAnalysisService 단위 테스트 보강"
```

---

### Task 21: N-M-03 — InactiveStaffLogoutInterceptor 세션 캐시

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/common/interceptor/InactiveStaffLogoutInterceptor.java`

**방침:** 최초 인증된 요청에서 `staff.isActive()`를 확인하고 결과를 `HttpSession` 어트리뷰트에 저장. 이후 요청에서는 세션에서 먼저 확인하고 DB 조회 생략.

```java
private static final String SESSION_KEY = "staffActive";

@Override
public boolean preHandle(...) {
    // 인증 없으면 통과
    ...
    HttpSession session = request.getSession(false);
    if (session != null) {
        Boolean cached = (Boolean) session.getAttribute(SESSION_KEY);
        if (cached != null) {
            if (!cached) {
                // 이미 비활성화 확인됨 → 로그아웃
                logoutHandler.logout(request, response, authentication);
                redirectToDeactivatedLogin(request, response);
                return false;
            }
            return true; // 활성화 확인됨 → DB 조회 생략
        }
    }
    // DB 조회 후 세션에 캐시
    return staffRepository.findByUsername(authentication.getName())
            .map(staff -> {
                boolean active = staff.isActive();
                if (session != null) session.setAttribute(SESSION_KEY, active);
                return handleStaff(request, response, authentication, staff);
            })
            .orElse(true);
}
```

- [ ] **Step 1: InactiveStaffLogoutInterceptor 세션 캐시 구현**

위 패턴으로 `preHandle` 메서드 수정.

- [ ] **Step 2: InactiveStaffLogoutInterceptorTest 실행**

```bash
./mvnw test -pl . -Dtest=InactiveStaffLogoutInterceptorTest -q 2>&1 | tail -10
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/smartclinic/hms/common/interceptor/InactiveStaffLogoutInterceptor.java
git commit -m "fix: N-M-03 InactiveStaffLogoutInterceptor 매 요청 DB 조회 → 세션 캐시로 개선"
```

---

### Task 22: L-01 + L-02 + L-03 — 저우선순위 개선

**Files:**
- Modify: `src/main/java/com/smartclinic/hms/domain/Doctor.java` (availableDays 정규화)
- Modify: `src/main/java/com/smartclinic/hms/common/interceptor/LayoutModelInterceptor.java` (boolean 플래그)
- Modify: `src/main/java/com/smartclinic/hms/domain/StaffRole.java` (getDashboardUrl)
- Modify: `src/main/java/com/smartclinic/hms/config/SecurityConfig.java` (switch 제거)

- [ ] **Step 1: L-03 — StaffRole에 getDashboardUrl() 추가**

```java
public enum StaffRole {
    ADMIN, DOCTOR, NURSE, STAFF, ITEM_MANAGER;

    public String getDashboardUrl() {
        return switch (this) {
            case ADMIN -> "/admin/dashboard";
            case DOCTOR -> "/doctor/dashboard";
            case NURSE -> "/nurse/dashboard";
            case STAFF -> "/staff/dashboard";
            case ITEM_MANAGER -> "/item-manager/dashboard";
        };
    }
}
```

`SecurityConfig`와 `LayoutModelInterceptor`의 중복 switch를 `role.getDashboardUrl()` 호출로 교체.

- [ ] **Step 2: L-02 — LayoutModelInterceptor boolean 플래그 정리**

26개 boolean 플래그(`isAdminDashboard` 등) 중 `path.contains()` 방식의 부정확한 매칭을 `path.startsWith()` 또는 정확한 패턴으로 교체.

- [ ] **Step 3: L-01 — Doctor.availableDays 공백 정규화**

`availableDays` setter 또는 저장 로직에서 공백 트리밍 처리:

```java
// Doctor 엔티티 setter 또는 AdminStaffService.save()에서
if (availableDays != null) {
    availableDays = Arrays.stream(availableDays.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .collect(Collectors.joining(","));
}
```

- [ ] **Step 4: 컴파일 + 전체 테스트**

```bash
./mvnw compile -pl . -am -q 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "fix: L-01/L-02/L-03 availableDays 정규화, LayoutModelInterceptor 플래그 정리, StaffRole.getDashboardUrl()"
```

---

## Final: 전체 테스트 실행 및 검증

### Task 23: 전체 테스트 + 빌드 최종 확인

- [ ] **Step 1: 전체 테스트 실행**

```bash
cd c:/workspace/gitstudy_lab/demo/team-demo/team-project-demo/hms
./mvnw test -pl . 2>&1 | tail -30
```
Expected: `BUILD SUCCESS`, `Tests run: N, Failures: 0, Errors: 0`

- [ ] **Step 2: 실패 테스트가 있으면 진단**

```bash
./mvnw test -pl . 2>&1 | grep -E "FAIL|ERROR|Exception" | head -30
```

- [ ] **Step 3: 코드 리뷰 이슈 체크리스트 확인**

각 이슈 ID별 수정 완료 여부를 `doc/CODE_REVIEW_2026-03-25.md` 기준으로 검토.

- [ ] **Step 4: 최종 Commit (미커밋 잔여분 처리)**

```bash
git status
git add -A
git commit -m "fix: HMS 코드 리뷰 29건 수정 완료 — CRITICAL 3 / HIGH 12 / MEDIUM 9 / LOW 5"
```

---

## 이슈 ID → Task 매핑 요약

| 이슈 ID | 심각도 | Task | 설명 |
|---------|--------|------|------|
| C-01 | CRITICAL | Task 1 | tools.jackson import |
| C-03 | CRITICAL | Task 2 | AdminStaffService 깨진 문자열 |
| C-02 / N-M-01 | CRITICAL / MEDIUM | Task 3 | innerHTML XSS |
| M-06 | MEDIUM | Task 4 | var → const/let |
| M-07 | MEDIUM | Task 5 | _sample @Profile |
| M-08 | MEDIUM | Task 6 | toKoreanDayOfWeek 제거 |
| H-08 | HIGH | Task 7 | 중복 CSS 삭제 |
| L-04, L-05 | LOW | Task 8 | 죽은 JS / nativeQuery |
| H-04 | HIGH | Task 9 | AdminItemApiController |
| H-09 | HIGH | Task 10 | NurseItemApiController |
| N-H-01 | HIGH | Task 11 | DoctorTreatmentApiController |
| N-H-03 | HIGH | Task 12 | ReceptionApiController |
| N-H-02, N-H-04 | HIGH | Task 13 | Model → HttpServletRequest |
| N-H-05, H-05, H-06 | HIGH | Task 14 | ReceptionService 로직 위임 + N+1 |
| H-07 | HIGH | Task 15 | NurseService N+1 |
| N-M-04 | MEDIUM | Task 16 | 에러 코드 기반 라우팅 |
| H-01 | HIGH | Task 17 | 다중 Repository 통합 |
| M-03, N-M-02 | MEDIUM | Task 18 | Clock 주입 |
| M-04 | MEDIUM | Task 19 | TestSecurityConfig 공통화 |
| M-05 | MEDIUM | Task 20 | LLM 서비스 테스트 |
| N-M-03 | MEDIUM | Task 21 | 인터셉터 세션 캐시 |
| L-01, L-02, L-03 | LOW | Task 22 | 저우선순위 개선 |
| - | - | Task 23 | 최종 테스트 확인 |
