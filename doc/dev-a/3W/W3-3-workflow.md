# W3-3 Workflow — 추천 결과 영역 시간 항목 추가

> **작성일**: 3W
> **목표**: 추천 결과 영역에 시간 항목 추가하여 진료과·의사·시간 모두 표시

---

## 전체 흐름

```
SYMPTOM_MAP에 time 필드 추가
  → 추천 결과 grid에 rec-time 카드 추가
  → callSymptomApi 결과에서 rec-time DOM 업데이트
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | 추천 결과에 시간 항목 추가 |
| 더미 데이터 | SYMPTOM_MAP에 time 필드 추가 |
| UI | 기존 진료과·의사 grid에 시간 카드 추가 |

---

## 실행 흐름

```
SYMPTOM_MAP에 time 필드 추가
  → analyzeSymptom() 반환값에 time 포함
  → callSymptomApi 결과에서 rec-time DOM 업데이트
```

---

## UI Mockup

```
┌──────────────────────────────────────┐
│ AI 추천 결과                          │
├─────────────┬──────────┬─────────────┤
│ 진료과      │ 담당의   │ 추천 시간   │  ← grid 3열
│ 내과        │ 이영희   │ 09:00       │
└─────────────┴──────────┴─────────────┘
```

---

## 작업 목록

1. HTML — 추천 결과 grid에 `rec-time` 카드 추가
2. JS — SYMPTOM_MAP 및 DEFAULT_RESULT에 `time` 필드 추가
3. JS — `analyzeSymptom()` 반환값에 `time` 포함
4. JS — callSymptomApi 결과에서 `rec-time` DOM 업데이트

---

## 작업 진행내용

- [x] HTML rec-time 카드 추가
- [x] SYMPTOM_MAP time 필드 추가
- [x] analyzeSymptom() time 반환
- [x] rec-time DOM 업데이트

---

## 실행 흐름에 대한 코드

### SYMPTOM_MAP — time 필드 추가

```javascript
const SYMPTOM_MAP = [
    { keywords: ['열', '기침', ...], dept: '내과', doctor: '김민준 원장', time: '09:00' },
    { keywords: ['수술', ...],       dept: '외과', doctor: '이성호 원장', time: '10:00' },
    ...
];
const DEFAULT_RESULT = { dept: '내과', doctor: '김민준 원장', time: '09:00' };
```

### DOM 업데이트

```javascript
document.getElementById('rec-dept').textContent   = result.dept;
document.getElementById('rec-doctor').textContent = result.doctor;
document.getElementById('rec-time').textContent   = result.time;  // 추가
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 키워드 매칭 | "기침이 심해요" | 내과 / 김민준 원장 / 09:00 |
| 기본값 | 매칭 없음 | 내과 / 김민준 원장 / 09:00 |

---

## 완료 기준

- [x] 추천 결과에 시간 항목 표시
- [x] SYMPTOM_MAP time 필드 반영
- [x] rec-time DOM 업데이트
