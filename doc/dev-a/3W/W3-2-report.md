# W3-2 리포트 - 증상 입력 JS fetch 호출 골격 구현

## 작업 개요
- **작업명**: `setTimeout` 방식을 async/await + Promise 기반 fetch 골격으로 교체
- **수정 파일**: `src/main/resources/templates/reservation/symptom-reservation.mustache`

## 작업 내용

### 1. callSymptomApi(text) async 함수 정의
W4 실제 연동 시 이 함수만 교체하면 되는 구조로 설계. 현재는 더미 Promise 반환.

```js
// W4에서 실제 fetch로 교체 예정 (주석 내 코드 참고)
async function callSymptomApi(symptomText) {
  return new Promise(resolve => {
    setTimeout(() => resolve(analyzeSymptom(symptomText)), 1500);
  });
}
```

W4 교체 시 아래 코드로 변경:
```js
async function callSymptomApi(symptomText) {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  return fetch('/llm/symptom/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrfToken },
    body: JSON.stringify({ symptom: symptomText })
  }).then(res => res.json());
}
```

### 2. analyzeBtn click 핸들러 async/await 구조로 변경
기존 `addEventListener('click', () => {})` → `addEventListener('click', async () => {})`로 변경.
`await callSymptomApi(text)` 호출로 비동기 결과 수신.

### 3. try/catch 오류 처리 골격 추가
API 실패 시 버튼 복구 처리. W3-5 폴백 UI(toast + 직접 선택 전환) 연결 예정.

```js
try {
  const result = await callSymptomApi(text);
  // DOM 업데이트
} catch (e) {
  // W3-5 폴백 UI 연결 예정
  analyzeBtn.disabled = false;
  analyzeBtn.innerHTML = '증상 분석하기';
}
```

## 테스트 결과
- 버튼 클릭 시 스피너 + "AI가 분석 중입니다..." 표시 ✅
- 1.5초 후 추천 결과 렌더링 ✅
- async/await 구조 정상 동작 ✅
