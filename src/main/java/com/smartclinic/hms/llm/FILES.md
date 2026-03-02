# llm 패키지 — 구성 파일 목록

> ◆ 경력자 소유 (Service) + ● 비전공자 B (UI 연결)

| 파일 | 설명 |
|------|------|
| LlmController.java | POST /llm/symptom/analyze |
| LlmService.java | Claude API 호출 (경력자 구현) |
| ChatbotController.java | POST /llm/chatbot/ask (비전공자 B UI 연결) |
| LlmRecommendationRepository.java | LLM 추천 이력 저장 |
