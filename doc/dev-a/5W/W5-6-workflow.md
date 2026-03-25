# W6-1 Workflow — 예약 컨트롤러 DTO 전달 방식 개선 + 코드 주석 보완

> **작성일**: 2026-03-23
> **브랜치**: `feature/dev-a`
> **목표**: 완료 화면 데이터를 URL 파라미터 대신 flash attribute(DTO)로 전달, a-skill.md 상세 주석 규칙 준수

---

## 전체 흐름

```
코드 리뷰 결과 두 가지 문제 확인
  1. ReservationCompleteInfo DTO가 뷰 모델로 활용되지 않음
     — redirectAttributes.addAttribute()로 각 필드를 URL 파라미터에 개별 노출
     — 완료 뷰에서 JS로 urlParams.get() 파싱
  2. a-skill.md 규칙 중 "코드에 상세 주석" 미준수
     — 메서드 내부 단계별 주석 없음

수정 방향
  → addFlashAttribute("info", info)로 DTO 통째로 전달
  → 완료 뷰 3개: JS URL 파싱 제거, Mustache {{#info}} 바인딩으로 교체
  → ReservationController / ReservationService 전 메서드 상세 주석 추가
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 문제 1 | 완료 화면 데이터가 URL 파라미터(name=, department= 등)로 노출됨 |
| 문제 2 | 메서드 내부 단계별 주석 없음 (a-skill.md 위반) |
| 수정 방식 | flash attribute → DTO 전달, Mustache 바인딩, 상세 주석 추가 |
| 영향 범위 | ReservationController, ReservationService, 완료 mustache 3개 |
| 기존 동작 유지 | PRG 패턴, @Valid, CSRF 변경 없음 |

---

## 실행 흐름

```
[변경 전]
POST /reservation/create
  → ReservationCompleteInfo info = service.createReservation(form)
  → redirectAttributes.addAttribute("name", info.getPatientName())   ← 필드 6개 개별 URL 파라미터
  → redirect:/reservation/complete?name=홍길동&department=내과&...
GET /reservation/complete
  → 모델 미사용, 뷰만 반환
reservation-complete.mustache
  → <script> urlParams.get('name') 로 DOM 조작

[변경 후]
POST /reservation/create
  → ReservationCompleteInfo info = service.createReservation(form)
  → redirectAttributes.addFlashAttribute("info", info)               ← DTO 통째로 세션 flash
  → redirect:/reservation/complete
GET /reservation/complete
  → flash attribute 자동으로 모델에 병합 (Spring MVC 처리)
reservation-complete.mustache
  → {{#info}}{{patientName}}, {{reservationNumber}} ...{{/info}}     ← Mustache 서버 렌더링
  → {{^info}} 오류 뷰 {{/info}}
```

---

## UI Mockup

```
[변경 전 — URL 노출]
/reservation/complete?name=홍길동&department=내과&doctor=김의사&date=2026-04-01&time=09:00

[변경 후 — 클린 URL]
/reservation/complete
(데이터는 서버 렌더링으로 HTML에 포함)

[완료 화면 렌더링 구조]
{{#info}}
  ┌──────────────────────────────────────┐
  │  예약이 완료되었습니다!               │
  │  {{patientName}}님의 진료 예약 접수  │
  │  예약번호: {{reservationNumber}}      │
  │  진료과 / 전문의: {{departmentName}} / {{doctorName}} │
  │  예약 일자: {{reservationDate}}       │
  │  예약 시간: {{timeSlot}}              │
  │  [메인으로 가기] [예약 조회 →]        │
  └──────────────────────────────────────┘
{{/info}}
{{^info}}
  ┌──────────────────────────────────────┐
  │  예약 정보가 없습니다.                │
  │  [처음으로 돌아가기]                  │
  └──────────────────────────────────────┘
{{/info}}
```

---

## 작업 목록

1. `ReservationController.java` — POST 3개 핸들러에서 `addAttribute` → `addFlashAttribute` 변경
2. `ReservationController.java` — GET 완료 화면 핸들러 `Model` → `HttpServletRequest` 통일
3. `ReservationController.java` — 전 메서드 단계별 상세 주석 추가
4. `ReservationService.java` — 전 메서드 단계별 상세 주석 추가
5. `reservation-complete.mustache` — JS URL 파싱 제거, `{{#info}}` Mustache 바인딩으로 교체
6. `reservation-cancel-complete.mustache` — 동일
7. `reservation-modify-complete.mustache` — 동일

---

## 작업 진행내용

- [x] ReservationController POST 3개 `addFlashAttribute` 변경
- [x] ReservationController 전 메서드 상세 주석 추가
- [x] ReservationService 전 메서드 상세 주석 추가
- [x] reservation-complete.mustache JS 제거 + Mustache 바인딩
- [x] reservation-cancel-complete.mustache JS 제거 + Mustache 바인딩
- [x] reservation-modify-complete.mustache JS 제거 + Mustache 바인딩

---

## 실행 흐름에 대한 코드

### 1. ReservationController — POST 핸들러 변경 (create 기준, cancel/modify 동일)

```java
// 예약 생성 → ReservationCompleteInfo DTO 반환
ReservationCompleteInfo info = reservationService.createReservation(form);
// flash attribute로 DTO 통째로 전달 (redirect 후 뷰에서 {{#info}}로 접근)
redirectAttributes.addFlashAttribute("info", info);
return "redirect:/reservation/complete";
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 생성 후 완료 화면으로 이동할 때, 완료 정보를 URL 파라미터가 아닌 flash attribute(임시 세션 저장소)로 전달합니다. redirect 이후 Spring MVC가 이 값을 자동으로 뷰 모델에 담아 Mustache 템플릿에서 `{{#info}}`로 접근할 수 있게 합니다.
> - **왜 이렇게 썼는지**: `addAttribute()`는 데이터를 URL 쿼리 파라미터(`?name=홍길동&department=내과`)로 노출시켜 개인정보가 브라우저 주소창에 표시됩니다. `addFlashAttribute()`는 데이터를 서버 세션에 한 번만 저장하고, redirect 완료 후 자동 삭제됩니다. PRG(Post-Redirect-Get) 패턴을 지키면서 개인정보를 URL에 노출하지 않는 안전한 방법입니다.
> - **쉽게 말하면**: 완료 정보를 "주소창에 적어서" 전달하던 것을 "봉투에 넣어 한 번만 전달"하는 방식으로 바꿔, 주소창에 이름/진료과 등이 노출되지 않도록 했습니다.

### 2. reservation-complete.mustache — Mustache 바인딩

```html
{{^info}}
<div class="text-center py-20">
  <h2 ...>예약 정보가 없습니다.</h2>
  <button onclick="location.href='/reservation'">처음으로 돌아가기</button>
</div>
{{/info}}

{{#info}}
<div class="max-w-2xl mx-auto">
  <span class="font-semibold">{{patientName}}</span>님의 진료 예약이 완료되었습니다.
  <div ...>{{reservationNumber}}</div>
  <div ...>{{patientName}}</div>
  <div ...>{{departmentName}} / {{doctorName}}</div>
  <div ...>{{reservationDate}}</div>
  <div ...>{{timeSlot}}</div>
  <a href="/reservation/lookup?reservationNumber={{reservationNumber}}">예약 조회</a>
</div>
{{/info}}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Mustache 조건문(`{{#info}}` / `{{^info}}`)으로 완료 화면을 서버에서 렌더링합니다. `info` 객체가 있으면 예약 완료 내용을 표시하고, 없으면(직접 URL 접근 시) "예약 정보가 없습니다" 오류 화면을 표시합니다.
> - **왜 이렇게 썼는지**: `{{#info}}...{{/info}}`는 "info가 존재할 때" 블록을 렌더링하는 Mustache 조건문입니다. `{{^info}}...{{/info}}`는 반대로 "info가 없을 때"를 의미합니다. 이전 방식(JS로 URL 파라미터 파싱 후 DOM 조작)에 비해 서버 렌더링이므로 더 안전하고 간결합니다. `{{reservationNumber}}`처럼 이중 중괄호 안의 변수명은 Java 객체의 필드명과 매칭됩니다.
> - **쉽게 말하면**: 서버가 HTML을 만들 때 예약 정보를 직접 채워 넣어 완성된 페이지를 보내주는 방식입니다. 브라우저에서 JavaScript로 데이터를 파싱하는 복잡한 과정이 없어집니다.

### 3. ReservationService — 주석 예시 (createReservation)

```java
// 동일 의사 + 날짜 + 시간에 CANCELLED 아닌 예약이 있으면 충돌 예외 (H-03)
if (reservationRepository.existsByDoctor_IdAnd...) { ... }

// 전화번호로 기존 환자 조회 → 없으면 신규 환자 등록
Patient patient = patientRepository.findByPhone(form.phone())
        .orElseGet(() -> patientRepository.save(...));

// 예약번호 생성 (RES-YYYYMMDD-XXX 형식) 및 예약 엔티티 저장
String reservationNumber = reservationNumberGenerator.generate(...);
reservationRepository.save(reservation);
// flush로 DB 제약 조건 위반을 즉시 감지
reservationRepository.flush();

// 완료 화면 표시에 필요한 정보만 DTO로 포장하여 반환 (트랜잭션 종료 후 LazyLoad 방지)
return new ReservationCompleteInfo(...);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 예약 생성 서비스 메서드의 핵심 단계들을 주석으로 설명합니다. ① 중복 예약 체크 → ② 환자 조회 또는 신규 등록 → ③ 예약번호 생성 및 저장 → ④ 완료 정보 DTO 반환의 순서입니다.
> - **왜 이렇게 썼는지**: `orElseGet()`은 값이 없을 때만 람다(함수)를 실행해 새 객체를 만들어 저장합니다. `flush()`는 JPA가 메모리에만 가지고 있던 변경 사항을 즉시 DB에 반영해, DB 제약 위반을 트랜잭션 끝이 아닌 이 시점에 감지합니다. `ReservationCompleteInfo` DTO를 반환하는 이유는, 트랜잭션이 끝난 후 엔티티의 연관 객체(LazyLoad)에 접근하면 오류가 발생하기 때문에 필요한 값만 미리 뽑아두는 것입니다.
> - **쉽게 말하면**: "이미 예약된 자리인지 확인 → 환자 등록(첫 방문이면 신규 등록) → 예약 번호표 발급 및 저장 → 완료 정보 정리해서 전달"의 4단계 흐름입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 예약 생성 완료 | POST /reservation/create 정상 | 완료 화면에 환자명·예약번호 등 표시 |
| 예약 취소 완료 | POST /reservation/cancel/{id} 정상 | 취소 완료 화면에 취소된 예약 정보 표시 |
| 예약 변경 완료 | POST /reservation/modify/{id} 정상 | 변경 완료 화면에 새 예약 정보 표시 |
| 직접 URL 접근 | GET /reservation/complete 직접 접근 | 오류 뷰("예약 정보가 없습니다.") 표시 |
| URL 파라미터 노출 없음 | 완료 redirect 후 URL 확인 | name=, department= 등 파라미터 없음 |

---

## 완료 기준

- [x] 완료 화면 URL에 개인정보(이름, 진료과 등) 파라미터 노출 없음
- [x] `{{#info}}` / `{{^info}}` Mustache 조건 정상 렌더링
- [x] 예약 조회 버튼이 올바른 reservationNumber로 링크 연결
- [x] ReservationController 전 메서드 단계별 주석 존재
- [x] ReservationService 전 메서드 단계별 주석 존재
