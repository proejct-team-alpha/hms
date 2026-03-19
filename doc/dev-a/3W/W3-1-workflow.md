# W3-1 증상 입력 JS 더미 비동기 처리

## 작업 목표
`symptom-reservation.mustache` script 블록을 수정하여 키워드 기반 더미 비동기 분석 처리 구현.
분석 버튼 클릭 시 추천 결과 영역이 올바르게 렌더링되도록 한다.

## 작업 목록

1. SYMPTOM_MAP 키워드 매핑 테이블 정의
2. analyzeSymptom(text) 함수 구현
3. analyzeBtn click 핸들러에서 analyzeSymptom 호출 후 DOM 업데이트
4. proceedToDirect() URL 버그 수정 (/reservation/direct-reservation)

## 수정 파일
- `src/main/resources/templates/reservation/symptom-reservation.mustache` (script 블록 전체 교체)

## 키워드 매핑 (더미)
| 키워드 | 진료과 | 담당의 |
|---|---|---|
| 열, 기침, 감기, 콧물, 목, 인후 | 내과 | 김민준 원장 |
| 수술, 상처, 외상, 찢김, 골절, 뼈 | 외과 | 이성호 원장 |
| 아이, 어린이, 소아, 유아 | 소아과 | 박지수 원장 |
| 귀, 코막힘, 이비인후, 코, 귀통증 | 이비인후과 | 최동우 원장 |
| (매칭 없음) | 내과 | 김민준 원장 |
