# W3-6 리포트 - 면책 고지 문구 구성

## 작업 개요
- **작업명**: 추천 결과 영역 하단에 AI 참고용 면책 고지 문구 추가
- **수정 파일**: `src/main/resources/templates/reservation/symptom-reservation.mustache`

## 작업 내용

### HTML — 면책 고지 문구 추가

"이 정보로 예약 진행하기" 버튼 하단에 추가.

```html
<p class="mt-4 text-xs text-slate-400 text-center flex items-center justify-center gap-1">
  <i data-feather="info" class="w-3 h-3"></i>
  AI 추천은 참고용이며, 의학적 진단이 아닙니다.
</p>
```

## 테스트 결과
- 추천 결과 영역 표시 시 면책 고지 문구 함께 표시 ✅
- feather info 아이콘 정상 렌더링 ✅

## 특이사항
- SKILL_DEV_A W3 체크포인트 "면책 고지 문구 표시" 조건 충족.
- W4 최종 확인 항목: 면책 문구 위치·스타일 유지 여부 재검증 예정.
