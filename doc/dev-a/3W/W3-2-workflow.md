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
