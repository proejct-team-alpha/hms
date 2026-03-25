# W3-1번째작업 Workflow — 접수 처리 후 의사 진료 목록 실시간 연동 (AJAX 폴링)

> **작성일**: 3W
> **목표**: 원무과 접수 처리(`RESERVED → RECEIVED`) 시 의사 진료 목록에 5초 폴링으로 실시간 반영

---

## 전체 흐름

```
원무과 접수 처리 → DB 상태 변경(RECEIVED) → 의사 화면 5초 폴링 → 진료 목록 자동 갱신
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 접수 처리 후 의사 진료 목록에 실시간 반영 필요 |
| 폴링 주기 | 5초 (setInterval) |
| 폴링 방식 | AJAX GET → JSON 응답 → 카드 목록 재렌더링 |
| 기존 구현 | `POST /staff/reception/receive`, `GET /doctor/treatment-list` SSR 완료 |
| 추가 필요 | `GET /doctor/treatment-list/poll` AJAX 엔드포인트, JS 폴링 핸들러 |

---

## 실행 흐름

```
setInterval(5000)
  → fetch GET /doctor/treatment-list/poll
  → 서버: Authentication으로 의사 식별 → RECEIVED 상태 오늘 목록 조회
  → 응답: { success: true, data: [...] }
  → JS: #treatment-card-list 카드 HTML 재구성
  → feather.replace() 아이콘 재렌더링
  → 오류 시: console.error (silent fail)
```

---

## UI Mockup

```
┌─────────────────────────────────────┐
│ 오늘의 진료 목록                      │
├──────┬────────┬───────┬─────────────┤
│ 순서  │ 환자명  │  시간  │     상태    │
├──────┼────────┼───────┼─────────────┤
│  1   │ 홍길동  │ 09:00 │  접수완료   │
│  2   │ 김철수  │ 09:30 │  접수완료   │
└──────┴────────┴───────┴─────────────┘
        ↑ 5초마다 자동 갱신 (AJAX 폴링)
```

---

## 작업 목록

1. `DoctorTreatmentService` — `getTodayReceivedList()` 메서드 추가
2. `DoctorTreatmentController` — `GET /doctor/treatment-list/poll` AJAX 엔드포인트 추가
3. `doctor/treatment-list.mustache` — `#treatment-card-list` id 부여 + 5초 폴링 JS 추가
4. `DoctorTreatmentServiceTest`, `DoctorTreatmentControllerTest` — 단위·MockMvc 테스트

---

## 작업 진행내용

- [x] `getTodayReceivedList()` 서비스 메서드 추가
- [x] `/poll` AJAX 엔드포인트 추가
- [x] 카드 목록 div에 `id="treatment-card-list"` 부여
- [x] `setInterval` 5초 폴링 JS 추가
- [x] 카드 HTML 동적 재구성 및 `feather.replace()` 호출
- [x] 단위·MockMvc 테스트 작성 및 통과

---

## 실행 흐름에 대한 코드

### 1. DoctorTreatmentService — getTodayReceivedList()

```java
// 오늘 날짜 기준 RECEIVED 상태 예약 목록 조회 (폴링용)
public List<DoctorReservationDto> getTodayReceivedList(String username) {
    LocalDate today = LocalDate.now();
    return reservationRepository
        .findByDoctorUsernameAndDateAndStatus(username, today, ReservationStatus.RECEIVED)
        .stream()
        .map(DoctorReservationDto::from)
        .toList();
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 오늘 날짜를 기준으로 접수 완료(`RECEIVED`) 상태인 예약 목록만 가져오는 서비스 메서드입니다. `username`으로 현재 로그인한 의사를 구분합니다.
> - **왜 이렇게 썼는지**: 폴링은 매 5초마다 실행되기 때문에 불필요한 데이터를 최소화해야 합니다. 오늘 날짜 + RECEIVED 상태만 조회해서 응답 크기를 줄입니다.
> - **쉽게 말하면**: 오늘 접수된 환자 목록만 가져오는 기능입니다.

### 2. DoctorTreatmentController — /poll 엔드포인트

```java
// GET /doctor/treatment-list/poll — 폴링 전용 AJAX 엔드포인트
@GetMapping("/treatment-list/poll")
@ResponseBody
public Resp<List<DoctorReservationDto>> pollTreatmentList(Authentication auth) {
    String username = auth.getName();
    List<DoctorReservationDto> data = doctorTreatmentService.getTodayReceivedList(username);
    return Resp.ok(data);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 브라우저가 5초마다 호출하는 폴링 전용 API 엔드포인트를 만듭니다. `@ResponseBody`는 결과를 JSON 형식으로 반환하라는 의미이고, `Authentication`은 현재 로그인한 사용자 정보를 자동으로 받아옵니다.
> - **왜 이렇게 썼는지**: 페이지 전체를 다시 불러오지 않고 데이터만 JSON으로 응답하여 화면 깜빡임 없이 목록을 갱신할 수 있습니다.
> - **쉽게 말하면**: 브라우저가 5초마다 "새 환자 없어요?" 하고 물어보면 서버가 JSON 데이터로 대답해주는 창구입니다.

### 3. treatment-list.mustache — 폴링 JS

```javascript
// 5초마다 /doctor/treatment-list/poll 호출 → 카드 목록 재렌더링
setInterval(async () => {
    try {
        const res = await fetch('/doctor/treatment-list/poll');
        if (!res.ok) return;
        const json = await res.json();
        const data = json.body || json;

        const container = document.getElementById('treatment-card-list');
        container.innerHTML = data.map(item => `
            <div class="...">
                <span>${item.patientName}</span>
                <span>${item.timeSlot}</span>
            </div>
        `).join('');
        feather.replace();
    } catch (e) {
        console.error('폴링 오류:', e);
    }
}, 5000);
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 페이지가 열려 있는 동안 5초마다 자동으로 서버에 최신 진료 목록을 요청하고, 응답받은 데이터로 화면의 카드 목록을 다시 그립니다.
> - **왜 이렇게 썼는지**: `setInterval`은 지정한 시간(5000ms = 5초)마다 함수를 반복 실행합니다. `id="treatment-card-list"`를 부여해야 JavaScript가 갱신할 위치를 정확히 찾을 수 있습니다. 에러 발생 시 silent fail(콘솔 로그만)로 처리해서 폴링 오류가 사용자 화면을 방해하지 않도록 합니다.
> - **쉽게 말하면**: 5초마다 "새 환자 목록 주세요"라고 서버에 요청하고, 받아온 데이터로 화면을 조용히 업데이트하는 자동 새로고침 기능입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| RECEIVED 목록 반환 | 오늘 RECEIVED 예약 존재 | 해당 목록 JSON 반환 |
| 빈 리스트 반환 | 오늘 RECEIVED 예약 없음 | 빈 배열 반환 |
| ROLE_DOCTOR 접근 | 인증된 DOCTOR 요청 | 200 + JSON 반환 |
| 미인증 접근 | 비로그인 요청 | 302 리다이렉트 |
| ROLE_STAFF 접근 | STAFF 역할로 요청 | 403 반환 |
| staff 접수 → 5초 반영 | 원무과 접수 처리 후 대기 | 5초 이내 doctor 화면 갱신 |

---

## 완료 기준

- [x] `getTodayReceivedList()` — 오늘 RECEIVED 목록 정상 반환
- [x] `GET /doctor/treatment-list/poll` — ROLE_DOCTOR 접근 성공 (200), 미인증 차단 (302)
- [x] 5초 폴링 — 접수 처리 후 의사 화면 자동 갱신 확인
- [x] 오류 발생 시 콘솔 로그만 출력 (화면 영향 없음)
