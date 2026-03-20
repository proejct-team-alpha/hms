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
