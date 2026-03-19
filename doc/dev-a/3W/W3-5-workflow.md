# W3-5 폴백 UI 구현

## 작업 목표
API 실패(catch) 시 오류 toast를 표시하고 3초 후 direct-reservation으로 자동 이동한다.

## 작업 목록

1. HTML: toast 알림 요소 추가 (#error-toast)
2. JS: catch 블록에 toast 표시 + 3초 후 /reservation/direct-reservation 이동

## 수정 파일
- `src/main/resources/templates/reservation/symptom-reservation.mustache`
