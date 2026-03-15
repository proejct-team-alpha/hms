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

### 2. DEPT_ID_MAP 추가 (direct-reservation)

진료과 이름 → numeric ID 변환 (select option은 numeric value 기반이므로 필요).

```js
const DEPT_ID_MAP = { '내과': '1', '외과': '2', '소아과': '3', '이비인후과': '4' };
```

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

### 4. 시간 select 자동 선택

```js
if (recTime) {
  document.getElementById('time').value = recTime;
}
```

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
