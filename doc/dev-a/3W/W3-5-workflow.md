# W3-5 Workflow — 폴백 UI 구현

> **작성일**: 3W
> **목표**: API 실패 시 toast 표시 + 3초 후 direct-reservation 자동 이동

---

## 전체 흐름

```
callSymptomApi() catch 블록
  → toast 알림 표시
  → 3초 후 /reservation/direct-reservation 이동
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 요청 | API 실패 시 사용자에게 안내 + 직접 예약 유도 |
| 표시 방법 | toast 알림 (#error-toast) |
| 자동 이동 | 3초 후 /reservation/direct-reservation |

---

## 실행 흐름

```
catch(err) 블록 진입
  → #error-toast 표시
  → setTimeout 3초 후 location.href = '/reservation/direct-reservation'
```

---

## UI Mockup

```
┌─────────────────────────────────────────────┐
│ ⚠️ AI 분석에 실패했습니다.                  │  ← toast (#error-toast)
│    잠시 후 직접 예약 화면으로 이동합니다.   │
└─────────────────────────────────────────────┘
```

---

## 작업 목록

1. HTML — toast 알림 요소 추가 (`#error-toast`)
2. JS — catch 블록에 toast 표시 + 3초 후 이동

---

## 작업 진행내용

- [x] #error-toast HTML 추가
- [x] catch 블록 toast + 자동 이동 구현

---

## 실행 흐름에 대한 코드

### HTML — toast 요소

```html
<div id="error-toast" class="hidden fixed bottom-4 left-1/2 -translate-x-1/2
     bg-red-500 text-white px-6 py-3 rounded-xl shadow-lg text-sm">
    AI 분석에 실패했습니다. 잠시 후 직접 예약 화면으로 이동합니다.
</div>
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 오류 발생 시 화면 하단 중앙에 빨간색 알림 메시지(토스트)를 표시하는 HTML 요소입니다. 기본적으로 `hidden` 클래스로 숨겨져 있다가 오류가 발생했을 때만 보여집니다.
> - **왜 이렇게 썼는지**: `fixed`는 스크롤해도 화면 특정 위치에 고정되는 CSS 속성입니다. `bottom-4`는 하단에서 4단위 위쪽, `left-1/2 -translate-x-1/2`는 수평 중앙 정렬을 의미합니다. 처음에는 숨겨두고(`hidden`) JS에서 필요할 때 클래스를 제거해 보여줍니다.
> - **쉽게 말하면**: 오류가 났을 때 화면 아래에 팝업처럼 뜨는 빨간 알림창입니다.

### JS — catch 블록

```javascript
} catch (err) {
    // API 실패 시 toast 표시 후 3초 후 직접 예약 화면으로 이동
    document.getElementById('error-toast').classList.remove('hidden');
    setTimeout(() => {
        location.href = '/reservation/direct-reservation';
    }, 3000);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: API 호출이 실패했을 때(`catch` 블록) 오류 토스트를 보여주고, 3초(3000ms) 후에 직접 예약 페이지로 자동 이동합니다.
> - **왜 이렇게 썼는지**: `classList.remove('hidden')`으로 숨겨진 토스트를 보이게 합니다. `setTimeout`으로 3초 뒤에 자동 이동하면 사용자가 메시지를 읽을 시간을 줄 수 있습니다. AI가 없어도 직접 예약할 수 있는 폴백(대안) 경로를 제공합니다.
> - **쉽게 말하면**: AI가 말을 못 하면 3초 후 자동으로 직접 예약 창구로 안내해주는 것입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| API 실패 | callSymptomApi() throw | toast 표시 |
| 자동 이동 | toast 표시 후 3초 | /reservation/direct-reservation 이동 |

---

## 완료 기준

- [x] API 실패 시 toast 표시
- [x] 3초 후 /reservation/direct-reservation 자동 이동
