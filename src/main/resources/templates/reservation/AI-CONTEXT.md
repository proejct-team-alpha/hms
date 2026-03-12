<!-- Parent: ../AI-CONTEXT.md -->

# templates/reservation

## 목적

비회원(환자) 예약 서비스 흐름을 위한 Mustache 템플릿.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| symptom-reservation.mustache | AI 증상 분석 기반 예약 시작 페이지 |
| direct-reservation.mustache | 직접 진료과 선택 예약 페이지 |
| reservation-lookup.mustache | 예약 번호/연락처 기반 예약 조회 |
| reservation-modify.mustache | 예약 수정 폼 |
| reservation-complete.mustache | 예약 완료 안내 페이지 |

## AI 작업 지침

- 예약 프로세스는 Step 단위로 진행되며, 각 단계별로 필요한 데이터가 모델에 담겨야 한다.
- AI 증상 분석 결과는 `LlmRecommendation` 도메인과 연동된다.
