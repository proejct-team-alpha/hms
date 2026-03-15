# W3-5 리포트 - 폴백 UI 구현

## 작업 개요
- **작업명**: API 실패 시 오류 toast 표시 + 3초 후 직접 선택 화면 자동 이동
- **수정 파일**: `src/main/resources/templates/reservation/symptom-reservation.mustache`

## 작업 내용

### 1. HTML — #error-toast 요소 추가

`</main>` 이전에 fixed position toast 요소를 추가.

```html
<div id="error-toast" class="hidden fixed bottom-6 left-1/2 -translate-x-1/2
     flex items-center gap-3 px-5 py-3 bg-slate-800 text-white rounded-xl shadow-lg text-sm z-50">
  <i data-feather="alert-circle" class="w-4 h-4 text-red-400 shrink-0"></i>
  <span>AI 분석에 실패했습니다. 잠시 후 직접 선택 화면으로 이동합니다.</span>
</div>
```

### 2. JS — catch 블록 폴백 처리

```js
} catch (e) {
  errorToast.classList.remove('hidden');
  feather.replace();
  setTimeout(() => {
    location.href = '/reservation/direct-reservation';
  }, 3000);
}
```

## 테스트 시나리오
- 더미 Promise는 항상 resolve이므로 catch가 실제로 트리거되지 않음 (W4 실제 fetch 연동 후 오류 시나리오 테스트 예정)
- W4에서 fetch timeout / 서버 오류 시 toast → 3초 후 이동 동작 확인 예정

## 특이사항
- 현재 `callSymptomApi()`는 항상 resolve이므로 폴백은 W4 연동 후 유효해진다.
- toast는 `feather.replace()` 재호출로 alert-circle 아이콘 렌더링.
