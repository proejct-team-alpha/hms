"""
요청/응답 스키마 정의
Spring Boot와 JSON 형식 협의
"""

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    """대화 이력 메시지"""
    role: str = Field(..., pattern="^(user|assistant)$")
    content: str = Field(..., min_length=1, max_length=4096)


class InferRequest(BaseModel):
    """LLM 추론 요청"""

    query: str = Field(..., description="사용자 쿼리 텍스트", min_length=1, max_length=4096)
    max_length: int = Field(default=512, ge=1, le=2048, description="생성 최대 토큰 수")
    temperature: float = Field(default=0.7, ge=0.0, le=2.0, description="생성 다양성 (0=결정적, 높을수록 다양)")
    top_p: float | None = Field(default=1.0, ge=0.0, le=1.0, description="nucleus sampling (선택)")
    num_return_sequences: int = Field(default=1, ge=1, le=5, description="생성 시퀀스 수")
    session_id: str | None = Field(default=None, description="세션 ID (대화 이력 추적용)")
    history: list[ChatMessage] | None = Field(default=None, max_length=20, description="이전 대화 이력, 최대 20개")

    model_config = {"json_schema_extra": {"examples": [{"query": "안녕하세요, 오늘 날씨는?"}]}}


class InferResponse(BaseModel):
    """LLM 추론 응답"""

    generated_text: str = Field(..., description="LLM이 생성한 응답 텍스트")

    model_config = {
        "json_schema_extra": {"examples": [{"generated_text": "안녕하세요! 오늘 날씨에 대해..."}]}
    }


class FeedbackRequest(BaseModel):
    """피드백 요청"""
    session_id: str | None = None
    query: str = Field(..., min_length=1, max_length=2048)
    response: str = Field(..., min_length=1, max_length=8192)
    score: int = Field(..., ge=1, le=5, description="만족도 (1-5)")
    comment: str | None = Field(default=None, max_length=1000)
    endpoint: str = Field(default="medical", pattern="^(medical|rule|infer|infer/medical|infer/rule)$")


class FeedbackResponse(BaseModel):
    """피드백 응답"""
    status: str = "ok"
    message: str = "피드백이 저장되었습니다"


class RuleIndexRequest(BaseModel):
    """규칙 인덱싱 요청 (Spring → Python LLM)"""
    rule_id: int = Field(..., description="hospital_rule 테이블의 ID")
    title: str = Field(..., min_length=1, max_length=200)
    content: str = Field(..., min_length=1, max_length=5000)
    category: str = Field(..., max_length=50, description="카테고리 (한글)")
    target: str | None = Field(default=None, max_length=100)
    active: bool = Field(default=True)


class RuleIndexResponse(BaseModel):
    """규칙 인덱싱 응답"""
    status: str = "ok"
    message: str = "규칙이 인덱싱되었습니다"
    rule_id: int
