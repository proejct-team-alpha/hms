# W3-1 리포트 - 증상 입력 JS 더미 비동기 처리

## 작업 개요
- **작업명**: 증상 입력 JS 비동기 처리를 더미 응답으로 구성하여 추천 결과 영역 렌더링
- **수정 파일**: `src/main/resources/templates/reservation/symptom-reservation.mustache`

## 작업 내용

### 1. SYMPTOM_MAP 키워드 매핑 테이블 정의
실제 드롭다운에 존재하는 4개 진료과 기준으로 키워드 매핑 테이블을 정의했다.

```js
const SYMPTOM_MAP = [
  { keywords: ['열', '기침', '감기', '콧물', '목', '인후'], dept: '내과',       doctor: '김민준 원장' },
  { keywords: ['수술', '상처', '외상', '찢김', '골절', '뼈'], dept: '외과',     doctor: '이성호 원장' },
  { keywords: ['아이', '어린이', '소아', '유아'],             dept: '소아과',    doctor: '박지수 원장' },
  { keywords: ['귀', '코막힘', '이비인후', '코', '귀통증'],  dept: '이비인후과', doctor: '최동우 원장' },
];
const DEFAULT_RESULT = { dept: '내과', doctor: '김민준 원장' };
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 4개의 진료과에 해당하는 증상 키워드 목록을 배열로 정의합니다. 각 항목에는 키워드 목록(`keywords`), 진료과(`dept`), 담당 의사(`doctor`)가 함께 들어 있습니다. 매칭이 없을 때 쓸 기본값(`DEFAULT_RESULT`)도 별도로 정의합니다.
> - **왜 이렇게 썼는지**: 실제 드롭다운에 존재하는 진료과와 동일하게 맞춰야 나중에 예약 폼에 자동 선택이 연결됩니다. 배열 방식으로 만들면 항목 추가·수정이 쉽습니다.
> - **쉽게 말하면**: 증상 → 진료과 안내판을 코드로 만든 것입니다.

### 2. analyzeSymptom(text) 함수 구현
증상 텍스트를 받아 SYMPTOM_MAP을 순차 탐색 후 첫 번째 매칭 결과를 반환한다. 매칭 없으면 기본값(내과) 반환.

```js
function analyzeSymptom(text) {
  for (const entry of SYMPTOM_MAP) {
    if (entry.keywords.some(kw => text.includes(kw))) {
      return { dept: entry.dept, doctor: entry.doctor };
    }
  }
  return DEFAULT_RESULT;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 입력된 증상 텍스트(`text`)를 받아 `SYMPTOM_MAP`을 처음부터 순서대로 탐색합니다. 각 항목의 키워드 중 하나라도 텍스트에 포함되면 해당 진료과와 의사 정보를 즉시 반환하고, 하나도 일치하지 않으면 기본값(내과)을 반환합니다.
> - **왜 이렇게 썼는지**: `for...of` 반복문은 배열을 순서대로 돌아봅니다. `some()`은 하나라도 조건을 만족하면 `true`를 반환하는 편리한 메서드입니다. 첫 번째 매칭에서 바로 반환하므로 불필요한 탐색을 하지 않아 효율적입니다.
> - **쉽게 말하면**: 증상 메모를 보고 첫 번째로 해당되는 진료과 창구를 찾아 안내해주는 안내원과 같습니다.

### 3. analyzeBtn click 핸들러 수정
버튼 클릭 시 `analyzeSymptom()`으로 결과를 먼저 결정한 뒤, 1.5초 setTimeout으로 로딩 연출 후 DOM을 업데이트한다.

- `rec-dept`, `rec-doctor` 텍스트를 매핑 결과로 업데이트
- `#recommendation-result` hidden 제거로 결과 영역 표시
- `feather.replace()` 호출로 아이콘 렌더링

### 4. proceedToDirect() URL 버그 수정
기존 코드의 잘못된 경로(`direct-reservation.html`)를 올바른 Spring MVC 경로로 수정했다.

| 구분 | 경로 |
|---|---|
| 수정 전 | `direct-reservation.html?dept=...&doctor=...` |
| 수정 후 | `/reservation/direct-reservation?dept=...&doctor=...` |

## 테스트 결과

| 입력 증상 | 기대 진료과 | 기대 담당의 | 결과 |
|---|---|---|---|
| "어제부터 열이 나고 기침이 심해요" | 내과 | 김민준 원장 | ✅ |
| "무릎 골절로 뼈가 아파요" | 외과 | 이성호 원장 | ✅ |
| "아이가 배가 아파요" | 소아과 | 박지수 원장 | ✅ |
| "귀가 많이 아파요" | 이비인후과 | 최동우 원장 | ✅ |
| "두통이 심해요" (매칭 없음) | 내과 | 김민준 원장 | ✅ |

- 버튼 클릭 시 스피너 + "AI가 분석 중입니다..." 표시 ✅
- 1.5초 후 추천 결과 영역 렌더링 ✅
- "이 정보로 예약 진행하기" → `/reservation/direct-reservation` 정상 이동 ✅

## 특이사항
- `소화기내과`, `안과`, `피부과`, `정형외과`는 현재 드롭다운에 없으므로 매핑에서 제외했다.
- `direct-reservation` 진료과 select의 numeric ID pre-fill은 이 작업 범위 밖이다 (향후 실제 LLM API 연동 시 처리 예정).
- 키워드 매칭은 순차 탐색(first-match) 방식이므로 배열 순서가 우선순위가 된다.
