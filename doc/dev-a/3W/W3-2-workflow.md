# W3-2 Workflow — 증상 입력 JS fetch 호출 골격 구현

> **작성일**: 3W
> **목표**: `setTimeout` 더미 → `async/await` + Promise 기반 fetch 골격으로 교체

---

## 전체 흐름

```
callSymptomApi() 함수를 async/await 구조로 교체
  → W4에서 실제 fetch 연동 시 callSymptomApi() 함수만 수정하면 되는 구조
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | setTimeout 더미 → async/await 골격 교체 |
| 목적 | W4 실제 API 연동 시 callSymptomApi()만 교체 |
| 오류 처리 | try/catch 골격 추가 (W3-5 폴백 UI 연결 예정) |

---

## 실행 흐름

```
analyzeBtn 클릭
  → await callSymptomApi(symptomText) 호출
  → (현재) 더미 Promise 반환
  → DOM 업데이트
  → catch: 오류 처리 (W3-5에서 구현)
```

---

## UI Mockup

```
[변경 없음 — JS 내부 구조만 변경]
```

---

## 작업 목록

1. `callSymptomApi(text)` async 함수 정의 (더미 Promise 반환)
2. `analyzeBtn` click 핸들러를 async/await 구조로 변경
3. try/catch 오류 처리 골격 추가

---

## 작업 진행내용

- [x] callSymptomApi() async 함수 정의
- [x] click 핸들러 async/await 구조로 변경
- [x] try/catch 골격 추가

---

## 실행 흐름에 대한 코드

### callSymptomApi() — async 더미

```javascript
async function callSymptomApi(symptomText) {
    // W4에서 실제 fetch로 교체 예정
    return new Promise(resolve => {
        setTimeout(() => resolve(analyzeSymptom(symptomText)), 1500);
    });
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 증상 텍스트를 받아 1.5초(1500ms) 뒤에 `analyzeSymptom()`의 결과를 돌려주는 함수입니다. `async` 키워드를 붙여 나중에 실제 서버 요청으로 교체하기 쉬운 구조로 만들었습니다.
> - **왜 이렇게 썼는지**: 실제 AI 서버가 아직 없기 때문에 `setTimeout`으로 1.5초 지연을 흉내 냈습니다. `Promise`는 "나중에 결과를 줄게"라는 약속이고, `resolve`는 그 약속을 이행하는 것입니다. `async` 함수로 감싸두면 나중에 내부만 바꿔도 호출부 코드를 수정하지 않아도 됩니다.
> - **쉽게 말하면**: 실제 AI가 없어도 마치 AI가 1.5초 동안 생각하는 것처럼 흉내 내는 가짜 함수입니다.

### analyzeBtn click 핸들러 — async/await

```javascript
analyzeBtn.addEventListener('click', async () => {
    try {
        const result = await callSymptomApi(symptomInput.value);
        document.getElementById('rec-dept').textContent   = result.dept;
        document.getElementById('rec-doctor').textContent = result.doctor;
        resultSection.classList.remove('hidden');
    } catch (err) {
        // W3-5 폴백 UI 연결 예정
        console.error(err);
    }
});
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 버튼 클릭 시 `callSymptomApi()`를 호출하고 결과가 올 때까지 기다린(`await`) 뒤, 진료과·담당의 텍스트를 업데이트하고 결과 영역을 보여줍니다. 오류 발생 시 `catch` 블록에서 처리합니다.
> - **왜 이렇게 썼는지**: `async/await`은 비동기 처리(시간이 걸리는 작업)를 마치 순서대로 실행되는 것처럼 쉽게 작성할 수 있는 문법입니다. `try/catch`는 오류가 발생했을 때 프로그램이 멈추지 않고 우아하게 처리할 수 있게 해줍니다.
> - **쉽게 말하면**: AI의 답변을 기다렸다가 받으면 화면에 보여주고, 문제가 생기면 조용히 오류를 기록하는 구조입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 분석 버튼 클릭 | 증상 입력 후 | 1.5초 후 결과 표시 |
| 오류 발생 | throw 테스트 | catch 블록 진입 |

---

## 완료 기준

- [x] callSymptomApi() async/await 구조
- [x] try/catch 골격 추가
- [x] W4 실제 fetch 연동 시 callSymptomApi()만 교체하면 되는 구조
