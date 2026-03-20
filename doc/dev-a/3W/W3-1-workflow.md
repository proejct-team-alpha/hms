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

### analyzeBtn click 핸들러

```javascript
analyzeBtn.addEventListener('click', () => {
    const result = analyzeSymptom(symptomInput.value);
    document.getElementById('rec-dept').textContent   = result.dept;
    document.getElementById('rec-doctor').textContent = result.doctor;
    resultSection.classList.remove('hidden');
});
```

### proceedToDirect() URL 수정

```javascript
function proceedToDirect() {
    location.href = `/reservation/direct-reservation?dept=${encodeURIComponent(recDept)}&doctor=${encodeURIComponent(recDoctor)}`;
}
```

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
