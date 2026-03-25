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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 추천 결과 버튼 아래에 작은 회색 글씨로 면책 고지 문구를 표시합니다. 정보 아이콘(info)이 앞에 붙어 있어 시각적으로 안내임을 알 수 있습니다.
> - **왜 이렇게 썼는지**: workflow의 박스형 디자인 대신 더 심플한 텍스트 형태로 구현했습니다. `text-slate-400`(연한 회색)은 주요 내용을 방해하지 않으면서 존재감을 유지합니다. `flex items-center justify-center`로 아이콘과 텍스트를 수평 중앙 정렬합니다.
> - **쉽게 말하면**: 버튼 아래에 작게 달려있는 "참고용입니다" 안내 문구입니다.

## 테스트 결과
- 추천 결과 영역 표시 시 면책 고지 문구 함께 표시 ✅
- feather info 아이콘 정상 렌더링 ✅

## 특이사항
- SKILL_DEV_A W3 체크포인트 "면책 고지 문구 표시" 조건 충족.
- W4 최종 확인 항목: 면책 문구 위치·스타일 유지 여부 재검증 예정.
