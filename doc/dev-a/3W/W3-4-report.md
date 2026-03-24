# W3-4 리포트 - 추천 → 예약 폼 연결 구조

## 작업 개요
- **작업명**: AI 추천 결과(진료과·의사·시간)를 direct-reservation 예약 폼에 자동 입력
- **수정 파일**:
  - `src/main/resources/templates/reservation/symptom-reservation.mustache`
  - `src/main/resources/templates/reservation/direct-reservation.mustache`

## 작업 내용

### 1. proceedToDirect()에 time 파라미터 추가 (symptom-reservation)

```js
function proceedToDirect() {
  const dept   = document.getElementById('rec-dept').textContent;
  const doctor = document.getElementById('rec-doctor').textContent;
  const time   = document.getElementById('rec-time').textContent;
  location.href = `/reservation/direct-reservation?dept=...&doctor=...&time=...`;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 결과 화면에 표시된 진료과·의사·시간을 읽어서 예약 페이지로 이동하는 URL을 만듭니다. W3-1 대비 time 파라미터가 추가됐습니다.
> - **왜 이렇게 썼는지**: 화면에 이미 렌더링된 값을 `textContent`로 읽는 것이 가장 간단하고 신뢰할 수 있는 방법입니다.
> - **쉽게 말하면**: 결과 카드에 표시된 세 가지 정보를 URL에 담아 예약 창구로 전달합니다.

### 2. DEPT_ID_MAP 추가 (direct-reservation)

진료과 이름 → numeric ID 변환 (select option은 numeric value 기반이므로 필요).

```js
const DEPT_ID_MAP = { '내과': '1', '외과': '2', '소아과': '3', '이비인후과': '4' };
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 진료과 이름(한글)을 숫자 ID로 변환하는 사전(객체)입니다. HTML select 요소의 option 값은 숫자이므로 이름 → 숫자 변환이 필요합니다.
> - **왜 이렇게 썼는지**: URL 파라미터에는 사람이 읽을 수 있는 이름("내과")이 오지만, select 요소의 value는 DB의 숫자 ID입니다. 이 맵으로 두 가지를 연결합니다.
> - **쉽게 말하면**: "내과"라는 이름표와 숫자 1번 번호표를 연결하는 대응표입니다.

### 3. 진료과 자동 선택 + 의사 fetch 후 이름 매칭 자동 선택

```js
const deptId = DEPT_ID_MAP[recDept];
deptSelect.value = deptId;

fetch(`/api/reservation/doctors?departmentId=${deptId}`)
  .then(res => res.json())
  .then(doctors => {
    // 의사 option 생성 후 이름 매칭으로 자동 선택
    const matched = Array.from(doctorSelect.options).find(o => o.textContent === recDoctor);
    if (matched) {
      doctorSelect.value = matched.value;
      doctorSelect.dispatchEvent(new Event('change')); // Flatpickr 갱신 트리거
    }
  });
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 진료과 ID로 서버에서 해당 진료과 의사 목록을 불러온 뒤, 추천된 의사 이름과 일치하는 option을 찾아 자동으로 선택합니다. 선택 후에는 `change` 이벤트를 직접 발생시켜 달력(Flatpickr)이 해당 의사의 근무 일정을 반영하도록 갱신합니다.
> - **왜 이렇게 썼는지**: 의사 목록은 서버 DB에서 오므로 fetch로 실시간 조회가 필요합니다. `dispatchEvent(new Event('change'))`는 사용자가 직접 선택했을 때와 동일한 효과를 코드로 만들어냅니다.
> - **쉽게 말하면**: 서버에 "이 진료과 의사 목록 주세요"라고 요청한 후, 받은 목록에서 추천 의사를 찾아 체크해주고 달력도 새로 그려주는 것입니다.

### 4. 시간 select 자동 선택

```js
if (recTime) {
  document.getElementById('time').value = recTime;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: URL 파라미터로 전달된 추천 시간(`recTime`)이 있으면 시간 선택 요소의 값을 해당 시간으로 설정합니다.
> - **왜 이렇게 썼는지**: `if (recTime)`으로 값이 있는 경우에만 실행합니다. 직접 예약 페이지에 URL 파라미터 없이 접근하는 경우에도 오류가 나지 않도록 방어합니다.
> - **쉽게 말하면**: 추천 시간이 있으면 시간 선택창을 그 시간으로 미리 맞춰주는 것입니다.

### 5. SYMPTOM_MAP 의사명 실제 DB 데이터로 수정

API `/api/reservation/doctors` 반환값(`staff.name`)과 일치하도록 수정.

| 진료과 | 수정 전 | 수정 후 |
|---|---|---|
| 내과 | 김민준 원장 | 의사이영희 |
| 외과 | 이성호 원장 | 의사김민준 |
| 소아과 | 박지수 원장 | 의사최지우 |
| 이비인후과 | 최동우 원장 | 의사이준혁 |

## 테스트 결과
- 증상 입력 → 분석 → "이 정보로 예약 진행하기" 클릭 시 direct-reservation 폼 자동 입력 ✅
- 진료과 select 자동 선택 ✅
- 전문의 select 자동 선택 (fetch 완료 후 이름 매칭) ✅
- 시간 select 자동 선택 ✅

## 특이사항
- 의사 fetch는 비동기이므로 진료과 선택 후 fetch 완료 시점에 doctor select가 자동 선택된다.
- `doctorSelect.dispatchEvent(new Event('change'))` 호출로 Flatpickr availableDays 갱신까지 연쇄 처리된다.
