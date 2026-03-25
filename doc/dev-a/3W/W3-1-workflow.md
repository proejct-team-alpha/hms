# W3-1 Workflow — 증상 입력 JS 더미 비동기 처리

> **작성일**: 3W
> **목표**: `symptom-reservation.mustache` 키워드 기반 더미 비동기 분석 처리 구현

---

## 전체 흐름

```
분석 버튼 클릭 → SYMPTOM_MAP 키워드 매핑 → DOM 업데이트
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 증상 키워드 기반 더미 분석 → 추천 결과 표시 |
| 실제 API | 추후 W4에서 연동 |
| 키워드 매핑 | 로컬 SYMPTOM_MAP 테이블 |
| URL 버그 | proceedToDirect() `/reservation/direct-reservation` 수정 |

---

## 실행 흐름

```
analyzeBtn 클릭
  → analyzeSymptom(text) 호출 (SYMPTOM_MAP 키워드 매칭)
  → 결과 DOM 업데이트 (진료과, 의사 표시)
  → proceedToDirect() URL 수정
```

---

## UI Mockup

```
┌─────────────────────────────────┐
│ 증상을 입력해주세요              │
│ [_____________________________ ]│
│              [분석하기]          │
├─────────────────────────────────┤
│ AI 추천 결과                    │
│ 진료과: 내과                    │
│ 담당의: 김민준 원장              │
│        [예약하러 가기]           │
└─────────────────────────────────┘
```

---

## 작업 목록

1. SYMPTOM_MAP 키워드 매핑 테이블 정의
2. `analyzeSymptom(text)` 함수 구현
3. `analyzeBtn` click 핸들러에서 `analyzeSymptom` 호출 후 DOM 업데이트
4. `proceedToDirect()` URL 버그 수정 (`/reservation/direct-reservation`)

---

## 작업 진행내용

- [x] SYMPTOM_MAP 정의
- [x] analyzeSymptom() 구현
- [x] analyzeBtn click 핸들러 DOM 업데이트
- [x] proceedToDirect() URL 수정

---

## 실행 흐름에 대한 코드

### SYMPTOM_MAP 키워드 매핑

```javascript
const SYMPTOM_MAP = [
    { keywords: ['열', '기침', '감기', '콧물', '목', '인후'], dept: '내과',    doctor: '김민준 원장' },
    { keywords: ['수술', '상처', '외상', '찢김', '골절', '뼈'], dept: '외과',   doctor: '이성호 원장' },
    { keywords: ['아이', '어린이', '소아', '유아'],              dept: '소아과', doctor: '박지수 원장' },
    { keywords: ['귀', '코막힘', '이비인후', '코', '귀통증'],    dept: '이비인후과', doctor: '최동우 원장' },
];
const DEFAULT_RESULT = { dept: '내과', doctor: '김민준 원장' };

function analyzeSymptom(text) {
    for (const entry of SYMPTOM_MAP) {
        if (entry.keywords.some(k => text.includes(k))) return entry;
    }
    return DEFAULT_RESULT;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `SYMPTOM_MAP`은 증상 키워드와 진료과·담당의를 연결하는 표(배열)입니다. `analyzeSymptom` 함수는 사용자가 입력한 증상 텍스트를 이 표와 비교해서 가장 먼저 일치하는 항목을 반환합니다. 아무것도 일치하지 않으면 기본값(내과)을 반환합니다.
> - **왜 이렇게 썼는지**: 실제 AI API 없이도 증상 분석 흐름을 흉내 낼 수 있도록 키워드 매핑 방식을 사용했습니다. `some()` 메서드는 배열 안에 조건을 만족하는 요소가 하나라도 있으면 `true`를 반환하므로 여러 키워드 중 하나라도 포함되면 해당 진료과로 연결됩니다.
> - **쉽게 말하면**: "기침"이라는 단어가 들어오면 내과로 안내해주는 단어 사전 역할을 합니다.

### analyzeBtn click 핸들러

```javascript
analyzeBtn.addEventListener('click', () => {
    const result = analyzeSymptom(symptomInput.value);
    document.getElementById('rec-dept').textContent   = result.dept;
    document.getElementById('rec-doctor').textContent = result.doctor;
    resultSection.classList.remove('hidden');
});
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: "분석하기" 버튼을 클릭하면 입력된 증상 텍스트로 `analyzeSymptom()`을 호출하고, 그 결과로 화면의 진료과·담당의 텍스트를 업데이트한 뒤 숨겨져 있던 결과 영역을 화면에 보이게 합니다.
> - **왜 이렇게 썼는지**: `addEventListener('click', ...)`은 버튼 클릭 이벤트를 감지하는 표준 방법입니다. `classList.remove('hidden')`은 Tailwind CSS의 `hidden` 클래스를 제거해서 요소를 화면에 표시합니다.
> - **쉽게 말하면**: 버튼을 누르면 분석 결과를 화면에 채워 넣고, 감춰진 결과 박스를 보여주는 역할입니다.

### proceedToDirect() URL 수정

```javascript
function proceedToDirect() {
    location.href = `/reservation/direct-reservation?dept=${encodeURIComponent(recDept)}&doctor=${encodeURIComponent(recDoctor)}`;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: "예약하러 가기" 버튼을 누르면 추천된 진료과와 의사 정보를 URL 주소에 담아서 예약 페이지로 이동합니다.
> - **왜 이렇게 썼는지**: `encodeURIComponent()`는 한글, 띄어쓰기 등 특수문자가 URL에서 깨지지 않도록 안전하게 변환해주는 함수입니다. URL에 데이터를 실어 보내는 것은 페이지 간에 간단한 정보를 전달하는 흔한 방법입니다.
> - **쉽게 말하면**: 추천 결과를 메모에 적어서 예약 페이지로 건네주는 것과 같습니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 키워드 매칭 | "기침이 심해요" | 내과 / 김민준 원장 |
| 키워드 없음 | "일반 증상" | 내과 / 김민준 원장 (기본값) |
| 예약하러 가기 | 버튼 클릭 | /reservation/direct-reservation 이동 |

---

## 완료 기준

- [x] SYMPTOM_MAP 키워드 기반 분석 동작
- [x] 추천 결과 DOM 업데이트
- [x] proceedToDirect() URL 정상 이동
