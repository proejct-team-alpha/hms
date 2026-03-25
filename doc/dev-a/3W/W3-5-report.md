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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 실제 서비스에 사용된 오류 토스트 HTML입니다. workflow의 기본 버전보다 더 세련된 디자인으로, 경고 아이콘(`alert-circle`)과 텍스트가 나란히 배치됩니다. `z-50`으로 다른 요소 위에 떠 있습니다.
> - **왜 이렇게 썼는지**: `flex items-center gap-3`으로 아이콘과 텍스트를 세로 중앙 정렬합니다. `bg-slate-800`(어두운 회색)은 workflow의 빨간 배경보다 덜 위협적인 느낌을 줍니다. `z-50`은 다른 요소들 위에 겹쳐 표시되도록 합니다.
> - **쉽게 말하면**: 경고 아이콘과 안내 문구가 함께 있는, 다른 내용 위에 뜨는 알림창입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: API 오류 발생 시 토스트를 보이게 하고(`classList.remove('hidden')`), Feather 아이콘 라이브러리를 재실행해 토스트 안의 아이콘을 렌더링한 뒤, 3초 후 직접 예약 페이지로 이동합니다.
> - **왜 이렇게 썼는지**: `feather.replace()`는 Feather 아이콘 라이브러리의 렌더링 함수입니다. 동적으로 추가된 요소의 아이콘은 초기 로드 시 렌더링되지 않으므로 숨겨진 요소가 보여질 때 다시 호출해야 합니다.
> - **쉽게 말하면**: 알림창을 보여주고 그 안의 아이콘도 제대로 그려준 다음, 3초 후에 예약 페이지로 자동으로 데려가는 코드입니다.

## 테스트 시나리오
- 더미 Promise는 항상 resolve이므로 catch가 실제로 트리거되지 않음 (W4 실제 fetch 연동 후 오류 시나리오 테스트 예정)
- W4에서 fetch timeout / 서버 오류 시 toast → 3초 후 이동 동작 확인 예정

## 특이사항
- 현재 `callSymptomApi()`는 항상 resolve이므로 폴백은 W4 연동 후 유효해진다.
- toast는 `feather.replace()` 재호출로 alert-circle 아이콘 렌더링.
