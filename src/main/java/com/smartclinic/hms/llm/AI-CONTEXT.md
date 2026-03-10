<!-- Parent: ../AI-CONTEXT.md -->

# llm — Claude API 연동

## 목적

Claude API를 사용한 증상 분석(AI 예약 추천)과 직원용 챗봇 기능.
**Service는 책임개발자 소유 / UI 연결(ChatbotController)은 개발자 B.**

## 주요 파일

| 파일 | 설명 |
|------|------|
| LlmController.java | POST /llm/symptom/analyze — 증상 분석 (비회원 접근 가능) |
| LlmService.java | Claude API 호출 (`analyzeSymptom()`, `askChatbot()`) — **수정 금지** |
| ChatbotController.java | POST /llm/chatbot/ask — 직원용 챗봇 (B 담당 UI) |
| LlmRecommendationRepository.java | LLM 추천 이력 저장 |
| dto/SymptomRequest.java | 증상 분석 요청 DTO |
| dto/SymptomResponse.java | 증상 분석 응답 DTO (`recommendedDept`, `recommendedDoctor`, `recommendedTime`) |
| dto/ChatbotRequest.java | 챗봇 질문 요청 DTO |
| dto/ChatbotResponse.java | 챗봇 응답 DTO |

## 핵심 동작

```
POST /llm/symptom/analyze
  → LlmController → LlmService.analyzeSymptom()
  → Claude API (5초 타임아웃)
  → 성공: SymptomResponse JSON
  → 실패/타임아웃: 폴백 응답 반환
```

## AI 작업 지침

- `LlmService` 수정 금지
- 5초 타임아웃 + 폴백 로직 이미 구현됨
- 증상 분석 엔드포인트는 비회원 허용 (`SecurityConfig` 설정)
- CSRF: AJAX POST이므로 CSRF 토큰 헤더 포함 필수

## 의존성

- 내부: `domain/LlmRecommendation`, `domain/ChatbotHistory`
- 외부: Claude API (RestClient), `config/ClaudeApiConfig`
