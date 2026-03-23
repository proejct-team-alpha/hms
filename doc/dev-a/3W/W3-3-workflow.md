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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 기존 `SYMPTOM_MAP`에 `time` 필드를 추가합니다. 이제 각 진료과별로 추천 시간 정보도 함께 가지게 됩니다. 기본값(`DEFAULT_RESULT`)에도 time 필드를 추가했습니다.
> - **왜 이렇게 썼는지**: 기존 구조에 필드 하나만 추가하면 되므로 코드 변경이 최소화됩니다. 더미 데이터이므로 각 진료과마다 고정된 시간을 부여했습니다.
> - **쉽게 말하면**: 기존 안내판에 "추천 시간" 열을 하나 더 추가한 것입니다.

### DOM 업데이트

```javascript
document.getElementById('rec-dept').textContent   = result.dept;
document.getElementById('rec-doctor').textContent = result.doctor;
document.getElementById('rec-time').textContent   = result.time;  // 추가
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 분석 결과에서 진료과·담당의·추천 시간을 각각 해당 HTML 요소의 텍스트로 채워 넣습니다. `getElementById`는 HTML에서 특정 `id`를 가진 요소를 찾는 함수입니다.
> - **왜 이렇게 썼는지**: `textContent`는 HTML 요소 안의 텍스트를 바꾸는 가장 안전한 방법입니다. 추천 시간(`rec-time`)을 새로 추가한 것 외에 나머지 두 줄은 기존 코드와 동일한 패턴입니다.
> - **쉽게 말하면**: 빈칸 세 개짜리 결과판에 진료과, 의사, 시간을 각각 채워 넣는 것입니다.

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
