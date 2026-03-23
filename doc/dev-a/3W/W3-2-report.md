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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 현재는 실제 서버에 요청하지 않고, 1.5초 뒤에 기존 `analyzeSymptom()` 결과를 돌려주는 임시 함수입니다. `async` 함수로 선언해두어 나중에 실제 서버 요청으로 쉽게 교체할 수 있습니다.
> - **왜 이렇게 썼는지**: 서버 연동 준비가 되지 않은 개발 초기 단계에서 UI 흐름을 먼저 완성하기 위해 더미(가짜) 구현을 사용합니다. 함수 이름과 반환 구조를 실제와 동일하게 맞춰두면 나중에 내부만 교체하면 됩니다.
> - **쉽게 말하면**: 진짜 주방이 아직 없어서 음식을 주문해도 1.5초 후에 미리 만들어둔 가짜 음식을 내오는 것과 같습니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 실제 AI 서버(`/llm/symptom/analyze`)에 증상 텍스트를 POST 방식으로 전송하고, JSON 형식의 분석 결과를 받아옵니다. CSRF 토큰도 함께 전송해 보안을 강화합니다.
> - **왜 이렇게 썼는지**: `fetch`는 브라우저에서 서버에 HTTP 요청을 보내는 표준 방법입니다. `X-CSRF-TOKEN`은 악의적인 다른 사이트가 사용자 대신 요청을 보내는 것을 막는 보안 장치입니다. `JSON.stringify`는 자바스크립트 객체를 서버가 이해할 수 있는 JSON 문자열로 변환합니다.
> - **쉽게 말하면**: 증상 메모를 AI 서버 우체통에 넣고 답장을 기다리는 과정입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: API 호출이 성공하면 결과로 화면을 업데이트하고, 실패하면 비활성화됐던 버튼을 다시 활성화하고 버튼 텍스트를 원래대로 복원합니다.
> - **왜 이렇게 썼는지**: 네트워크 오류나 서버 오류로 API 호출이 실패할 수 있습니다. `try/catch`로 오류를 잡아내면 사용자가 화면이 멈춘 것처럼 느끼지 않고 다시 시도할 수 있도록 버튼 상태를 복구할 수 있습니다.
> - **쉽게 말하면**: 요청이 실패하면 "다시 해보세요"라고 버튼을 되살려주는 오류 복구 코드입니다.

## 테스트 결과
- 버튼 클릭 시 스피너 + "AI가 분석 중입니다..." 표시 ✅
- 1.5초 후 추천 결과 렌더링 ✅
- async/await 구조 정상 동작 ✅
