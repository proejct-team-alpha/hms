# W3-4 Workflow — 추천 → 예약 폼 연결 구조

> **작성일**: 3W
> **목표**: 추천 결과의 time 파라미터를 direct-reservation으로 전달 + 자동 선택

---

## 전체 흐름

```
symptom-reservation 추천 결과
  → proceedToDirect()에 time 파라미터 추가
  → direct-reservation URL 파라미터 수신
  → 진료과·의사·시간 select 자동 선택
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 추천 결과에서 예약 폼으로 자동 채움 연결 |
| 진료과 자동 선택 | DEPT_ID_MAP으로 이름→ID 변환 후 select |
| 의사 자동 선택 | fetch 후 이름 매칭으로 select |
| 시간 자동 선택 | time select 자동 선택 |
| SYMPTOM_MAP 의사명 | 실제 DB 데이터로 수정 |

---

## 실행 흐름

```
symptom-reservation: proceedToDirect()
  → URL: /reservation/direct-reservation?dept=내과&doctor=이름&time=09:00
  ↓
direct-reservation: DOMContentLoaded
  → URL 파라미터 파싱
  → DEPT_ID_MAP으로 진료과 ID 변환 → department select 자동 선택
  → fetch /api/reservation/doctors → 이름 매칭 → doctor select 자동 선택
  → time select 자동 선택
```

---

## UI Mockup

```
[symptom-reservation → direct-reservation 자동 채움]

직접 선택 예약
┌─────────────────────────────────┐
│ 진료과  [내과 ▼            ] ← 자동 선택
│ 전문의  [의사이영희 ▼     ] ← 자동 선택
│ 날짜    [__________________]
│ 시간    [09:00 ▼          ] ← 자동 선택
└─────────────────────────────────┘
```

---

## 작업 목록

1. `symptom-reservation.mustache` — `proceedToDirect()`에 time 파라미터 추가
2. `direct-reservation.mustache` — DEPT_ID_MAP으로 진료과 이름→ID 변환 후 자동 선택
3. `direct-reservation.mustache` — 의사 fetch 후 이름 매칭으로 doctor select 자동 선택
4. `direct-reservation.mustache` — time select 자동 선택
5. `symptom-reservation.mustache` — SYMPTOM_MAP 의사명을 실제 DB 데이터로 수정

---

## 작업 진행내용

- [x] proceedToDirect() time 파라미터 추가
- [x] DEPT_ID_MAP 진료과 자동 선택
- [x] 의사 fetch 이름 매칭 자동 선택
- [x] time select 자동 선택
- [x] SYMPTOM_MAP 의사명 실제 DB 데이터로 수정

---

## 실행 흐름에 대한 코드

### symptom-reservation — proceedToDirect()

```javascript
function proceedToDirect() {
    const dept   = document.getElementById('rec-dept').textContent;
    const doctor = document.getElementById('rec-doctor').textContent;
    const time   = document.getElementById('rec-time').textContent;
    location.href = `/reservation/direct-reservation?dept=${encodeURIComponent(dept)}&doctor=${encodeURIComponent(doctor)}&time=${encodeURIComponent(time)}`;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 추천 결과 화면에 표시된 진료과·의사·시간 텍스트를 읽어서, 예약 페이지 URL에 파라미터로 붙여 이동합니다. W3-1에서 2개 파라미터였던 것을 `time`을 추가해 3개로 확장했습니다.
> - **왜 이렇게 썼는지**: 화면에 이미 표시된 텍스트를 다시 변수에 담아 URL로 전달합니다. `encodeURIComponent()`로 한글과 특수문자를 URL 안전 형식으로 변환합니다.
> - **쉽게 말하면**: 추천 결과 쪽지에 시간 정보까지 추가로 적어서 예약 창구로 넘겨주는 것입니다.

### direct-reservation — URL 파라미터 자동 채움

```javascript
const DEPT_ID_MAP = { '내과': '1', '외과': '2', '소아과': '3', '이비인후과': '4' };

const urlParams = new URLSearchParams(window.location.search);
const recDept   = urlParams.get('dept');
const recDoctor = urlParams.get('doctor');
const recTime   = urlParams.get('time');

if (recDept) {
    // 진료과 자동 선택
    const deptId = DEPT_ID_MAP[recDept];
    document.getElementById('department').value = deptId;
    // 의사 fetch 후 이름 매칭
    fetch(`/api/reservation/doctors?departmentId=${deptId}`)
        .then(res => res.json())
        .then(doctors => {
            doctors.forEach(d => { /* option 추가 */ });
            if (recDoctor) document.getElementById('doctor').value = /* 이름 매칭 */;
            if (recTime) document.getElementById('time').value = recTime;
        });
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 이전 페이지에서 전달한 URL 파라미터(진료과·의사·시간)를 읽어 예약 폼을 자동으로 채웁니다. 진료과 이름을 숫자 ID로 변환한 뒤(`DEPT_ID_MAP`) select를 선택하고, 해당 진료과 의사 목록을 서버에서 불러와 이름이 일치하는 의사를 자동 선택합니다.
> - **왜 이렇게 썼는지**: `URLSearchParams`는 URL의 `?` 이후 파라미터를 쉽게 파싱하는 브라우저 내장 API입니다. select 요소는 option의 숫자 value로 선택하므로 이름 → ID 변환 맵(`DEPT_ID_MAP`)이 필요합니다. 의사 목록은 서버 데이터와 동기화해야 하므로 fetch로 불러옵니다.
> - **쉽게 말하면**: 앞 페이지에서 받은 쪽지를 보고 예약 양식을 대신 채워주는 자동완성 기능입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 증상 분석 후 이동 | 추천 결과에서 예약 클릭 | 진료과/의사/시간 자동 선택 |
| 직접 접근 | URL 파라미터 없이 접근 | 기본 빈 폼 표시 |

---

## 완료 기준

- [x] proceedToDirect() time 파라미터 포함
- [x] direct-reservation 진료과 자동 선택
- [x] 의사 fetch 후 이름 매칭 자동 선택
- [x] time select 자동 선택
