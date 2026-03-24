"""
LLM 추론 서버 - FastAPI 앱
PRD 기반: Spring Boot에서 HTTP 호출, RAG 없이 순수 LLM 추론
"""

import asyncio
import logging
import time as _time
from contextlib import asynccontextmanager

import json

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, StreamingResponse

import httpx

from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.util import get_remote_address
from slowapi.errors import RateLimitExceeded

from circuit_breaker import ServiceUnavailableError
from metrics import metrics
from config import get_settings
from medical_context_service import build_medical_context, close_pool, get_pool
from prompt_loader import load_prompt
from response_cleaner import NON_KOREAN_CJK_PATTERN, SPECIAL_TOKEN_PATTERN, clean_llm_response
import aiomysql
from schemas import InferRequest, InferResponse, FeedbackRequest, FeedbackResponse, RuleIndexRequest, RuleIndexResponse
from typo_corrector import correct_typos_async

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# LLM 생성 중단 토큰 (공통 상수)
STOP_TOKENS = ["<|im_start|>", "<|im_end|>", "<|endoftext|>", "，。，", "。，。"]


@asynccontextmanager
async def lifespan(app: FastAPI):
    """앱 시작/종료 시 리소스 관리 (lazy initialization)"""
    # startup — MySQL과 ChromaDB 모두 lazy init (첫 요청 시 연결)
    settings = get_settings()
    logger.info("Server starting (MySQL: %s:%d, ChromaDB: %s:%d, vector_search: %s)",
                settings.mysql_host, settings.mysql_port,
                settings.chroma_host, settings.chroma_port,
                settings.use_vector_search)

    app.state.http_client = httpx.AsyncClient(
        timeout=httpx.Timeout(
            connect=5.0,
            read=float(settings.llm_infer_timeout_sec),
            write=5.0,
            pool=5.0,
        ),
        limits=httpx.Limits(max_connections=20, max_keepalive_connections=10),
    )
    logger.info("Shared httpx client created")

    yield

    # shutdown
    await app.state.http_client.aclose()
    await close_pool()
    logger.info("Resources cleaned up")


app = FastAPI(
    title="Python LLM Inference API",
    description="Spring Boot 연동용 LLM 추론 서버",
    version="0.1.0",
    lifespan=lifespan,
)

# Rate Limiting
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# CORS: Spring Boot 연동 시 허용 origin 제한 (환경변수 CORS_ORIGINS으로 재정의 가능)
_settings = get_settings()
app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in _settings.cors_origins.split(",") if o.strip()],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["Content-Type", "Authorization", "X-Session-Id"],
)


# ──────────────────────────────────────────────
# 헬퍼 함수
# ──────────────────────────────────────────────

def _build_history_messages(history: list | None) -> list[dict]:
    """대화 이력에서 최근 3턴(6개) 메시지를 추출한다."""
    if not history:
        return []
    messages = []
    recent = history[-6:]
    for msg in recent:
        role = msg.role if hasattr(msg, "role") else msg.get("role")
        content = msg.content if hasattr(msg, "content") else msg.get("content")
        if role in ("user", "assistant") and content:
            messages.append({"role": role, "content": content})
    return messages


def _build_medical_messages(corrected_query: str, medical_context: str, history: list | None) -> list[dict]:
    """의학 추론용 메시지 리스트를 조합한다."""
    messages = [{"role": "system", "content": load_prompt("medical_system")}]
    if medical_context:
        messages.append({"role": "system", "content": medical_context})
    messages.extend(_build_history_messages(history))
    messages.append({"role": "user", "content": corrected_query})
    return messages


def _build_rule_messages(corrected_query: str, rule_context: str, history: list | None) -> list[dict]:
    """병원규칙 추론용 메시지 리스트를 조합한다."""
    messages = [{"role": "system", "content": load_prompt("rule_system")}]
    messages.append({"role": "system", "content": rule_context})
    messages.extend(_build_history_messages(history))
    messages.append({"role": "user", "content": corrected_query})
    return messages


async def _chat_llm(messages: list[dict], body: InferRequest, client: httpx.AsyncClient) -> str:
    """vLLM 또는 Ollama Chat API를 호출한다."""
    settings = get_settings()
    if settings.llm_backend == "vllm":
        from vllm_service import chat_with_vllm
        return await chat_with_vllm(
            messages=messages,
            temperature=body.temperature,
            max_length=body.max_length,
            stop=STOP_TOKENS,
            client=client,
        )
    else:
        from ollama_service import chat_with_ollama
        return await chat_with_ollama(
            messages=messages,
            temperature=body.temperature,
            max_length=body.max_length,
            stop=STOP_TOKENS,
            client=client,
        )


def _get_chat_stream_fn():
    """스트리밍용 chat 함수를 반환한다."""
    settings = get_settings()
    if settings.llm_backend == "vllm":
        from vllm_service import chat_with_vllm_stream
        return chat_with_vllm_stream
    else:
        from ollama_service import chat_with_ollama_stream
        return chat_with_ollama_stream


def _make_sse_generator(chat_stream_fn, messages: list[dict], body: InferRequest, client: httpx.AsyncClient):
    """SSE 스트리밍 제너레이터를 생성한다."""
    async def generate_sse():
        try:
            async for item in chat_stream_fn(
                messages=messages,
                temperature=body.temperature,
                max_length=body.max_length,
                stop=STOP_TOKENS,
                client=client,
            ):
                if "token" in item:
                    raw_token = item["token"]
                    token = SPECIAL_TOKEN_PATTERN.sub("", raw_token)
                    token = NON_KOREAN_CJK_PATTERN.sub("", token)
                    if token:
                        data = json.dumps({"token": token}, ensure_ascii=False)
                        yield f"data: {data}\n\n"
                if item.get("done"):
                    yield "data: [DONE]\n\n"
        except Exception as exc:
            logger.error("Stream error: %s", exc)
            yield f"data: {json.dumps({'error': '응답 생성 중 오류가 발생했습니다'}, ensure_ascii=False)}\n\n"
    return generate_sse


def _streaming_response(generator):
    """SSE StreamingResponse를 생성한다."""
    return StreamingResponse(
        generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )


# ──────────────────────────────────────────────
# 엔드포인트
# ──────────────────────────────────────────────

@app.get("/")
def root():
    """루트: 서버 상태 확인"""
    return {"status": "ok", "message": "LLM Inference API"}


@app.get("/metrics")
@limiter.limit("30/minute")
def get_metrics(request: Request):
    """추론 메트릭 조회"""
    return metrics.to_dict()


@app.post("/typo/reload")
@limiter.limit("2/minute")
async def typo_reload(request: Request):
    """오타 사전 DB에서 리로드"""
    from typo_corrector import reload_typo_dict
    await reload_typo_dict()
    return {"status": "ok", "message": "Typo dictionary reloaded"}


async def _check_mysql_health() -> bool:
    """MySQL 연결 상태 확인"""
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor() as cur:
                await cur.execute("SELECT 1")
                return True
    except Exception:
        return False


def _check_chromadb_health() -> bool:
    """ChromaDB 연결 상태 확인"""
    try:
        from vector_store import get_chroma_client
        client = get_chroma_client()
        client.heartbeat()
        return True
    except Exception:
        return False


@app.get("/health")
async def health():
    """헬스체크 엔드포인트 — LLM 백엔드, MySQL, ChromaDB 상태 확인"""
    settings = get_settings()

    checks = {}

    # LLM 백엔드 체크
    if settings.llm_backend == "vllm":
        from vllm_service import check_vllm_health
        checks["vllm"] = await check_vllm_health()
    elif settings.llm_backend == "ollama":
        from ollama_service import check_ollama_health
        checks["ollama"] = await check_ollama_health()

    # MySQL 체크
    checks["mysql"] = await _check_mysql_health()

    # ChromaDB 체크 (동기 호출을 to_thread로 감싸서 이벤트 루프 블로킹 방지)
    checks["chromadb"] = await asyncio.to_thread(_check_chromadb_health)

    all_healthy = all(checks.values())

    # Circuit Breaker 상태
    if settings.llm_backend == "vllm":
        from vllm_service import _breaker
    else:
        from ollama_service import _breaker
    return {
        "status": "healthy" if all_healthy else "degraded",
        "llm_backend": settings.llm_backend,
        "checks": checks,
        "circuit_breaker": _breaker.state,
    }


@app.exception_handler(TimeoutError)
def timeout_handler(request: Request, exc: TimeoutError):
    """추론 타임아웃 시 503 반환"""
    logger.warning("Request timeout: %s", exc)
    return JSONResponse(status_code=503, content={"detail": "요청 처리 시간이 초과되었습니다"})


@app.exception_handler(MemoryError)
def memory_error_handler(request: Request, exc: MemoryError):
    """OOM 시 503 반환"""
    logger.error("Out of memory: %s", exc)
    return JSONResponse(status_code=503, content={"detail": "LLM 추론 중 메모리 부족"})


@app.exception_handler(RuntimeError)
def runtime_error_handler(request: Request, exc: RuntimeError):
    """모델/CUDA 오류 등 503 반환"""
    logger.error("Runtime error: %s", exc)
    return JSONResponse(status_code=503, content={"detail": "LLM 런타임 오류가 발생했습니다"})


@app.exception_handler(ConnectionError)
def connection_error_handler(request: Request, exc: ConnectionError):
    """Ollama/외부 서버 연결 실패 시 503 반환"""
    logger.error("Connection error: %s", exc)
    return JSONResponse(status_code=503, content={"detail": "LLM 서버에 연결할 수 없습니다. 서버 실행 여부를 확인하세요."})


@app.exception_handler(OSError)
def os_error_handler(request: Request, exc: OSError):
    """모델 로딩 실패(DLL 등) 503 반환"""
    logger.error("OS error: %s", exc)
    if isinstance(exc, ConnectionError):
        return JSONResponse(status_code=503, content={"detail": "LLM 서버에 연결할 수 없습니다. 서버 실행 여부를 확인하세요."})
    return JSONResponse(status_code=503, content={"detail": "LLM 모델 로딩 실패"})


@app.exception_handler(ServiceUnavailableError)
def circuit_breaker_handler(request: Request, exc: ServiceUnavailableError):
    """Circuit Breaker OPEN 시 503 반환"""
    logger.warning("Circuit breaker rejected request: %s", exc)
    return JSONResponse(
        status_code=503,
        content={"detail": "LLM 서비스가 일시적으로 중단되었습니다. 잠시 후 다시 시도해 주세요."},
        headers={"Retry-After": "30"},
    )


@app.exception_handler(Exception)
def general_exception_handler(request: Request, exc: Exception):
    """기타 예외 500 반환"""
    logger.exception("Unhandled exception: %s", exc)
    return JSONResponse(status_code=500, content={"detail": "서버 내부 오류가 발생했습니다"})


@app.post("/infer", response_model=InferResponse)
@limiter.limit("20/minute")
async def infer(request: Request, body: InferRequest) -> InferResponse:
    """
    LLM 추론: 쿼리 입력 → 생성 응답 반환
    Spring Boot /api/llm/query 에서 호출
    LLM_BACKEND=ollama 시 Ollama 서버, 아니면 Hugging Face 사용
    """
    _start = _time.time()
    query_preview = body.query[:50] + "..." if len(body.query) > 50 else body.query
    logger.info("Infer request: query=%s", repr(query_preview))

    settings = get_settings()

    corrected = await correct_typos_async(body.query)

    try:
        if settings.llm_backend == "vllm":
            from vllm_service import generate_with_vllm

            generated_text = await generate_with_vllm(
                query=corrected,
                max_length=body.max_length,
                temperature=body.temperature,
                top_p=body.top_p or 1.0,
                client=request.app.state.http_client,
            )
        elif settings.llm_backend == "ollama":
            from ollama_service import generate_with_ollama

            generated_text = await generate_with_ollama(
                query=corrected,
                max_length=body.max_length,
                temperature=body.temperature,
                top_p=body.top_p or 1.0,
                client=request.app.state.http_client,
            )
        else:
            from llm_service import generate

            generated_text = await asyncio.to_thread(
                generate,
                query=corrected,
                max_length=body.max_length,
                temperature=body.temperature,
                top_p=body.top_p or 1.0,
                num_return_sequences=body.num_return_sequences,
            )
    except Exception as exc:
        _elapsed = (_time.time() - _start) * 1000
        fallback = settings.llm_fallback_response
        if fallback:
            logger.warning("LLM failed: %s, using fallback response", exc)
            generated_text = fallback
        else:
            metrics.record_request(latency_ms=_elapsed, success=False)
            raise

    _elapsed = (_time.time() - _start) * 1000
    metrics.record_request(latency_ms=_elapsed, success=True)
    logger.info("Infer response: length=%d", len(generated_text))
    return InferResponse(generated_text=generated_text)


@app.post("/infer/medical", response_model=InferResponse)
@limiter.limit("10/minute")
async def infer_medical(request: Request, body: InferRequest) -> InferResponse:
    """
    의학지식 데이터 기반 LLM 추론
    1. MySQL에서 관련 의학 데이터 실시간 조회
    2. 시스템 프롬프트 + 의학 컨텍스트 + 사용자 질문 조합
    3. Ollama Chat API 호출
    """
    _start = _time.time()
    query_preview = body.query[:50] + "..." if len(body.query) > 50 else body.query
    logger.info("Medical infer request: query=%s", repr(query_preview))

    corrected_query = await correct_typos_async(body.query)
    medical_context = await build_medical_context(corrected_query)
    logger.info("Medical context: %d chars", len(medical_context))

    messages = _build_medical_messages(corrected_query, medical_context, body.history)

    try:
        raw_text = await _chat_llm(messages, body, request.app.state.http_client)
        generated_text = clean_llm_response(raw_text)
    except Exception:
        _elapsed = (_time.time() - _start) * 1000
        metrics.record_request(latency_ms=_elapsed, success=False, vector_hit=len(medical_context) > 0)
        raise

    _elapsed = (_time.time() - _start) * 1000
    metrics.record_request(latency_ms=_elapsed, success=True, vector_hit=len(medical_context) > 0)
    logger.info("Medical infer response: length=%d", len(generated_text))
    return InferResponse(generated_text=generated_text)


@app.post("/infer/medical/stream")
@limiter.limit("10/minute")
async def infer_medical_stream(request: Request, body: InferRequest):
    """
    의학지식 기반 LLM 스트리밍 추론 (SSE)
    Ollama stream:true → Server-Sent Events로 토큰 단위 전송
    """
    query_preview = body.query[:50] + "..." if len(body.query) > 50 else body.query
    logger.info("Medical stream request: query=%s", repr(query_preview))

    corrected_query = await correct_typos_async(body.query)
    medical_context = await build_medical_context(corrected_query)
    logger.info("Medical context (stream): %d chars", len(medical_context))

    messages = _build_medical_messages(corrected_query, medical_context, body.history)
    chat_stream_fn = _get_chat_stream_fn()
    generator = _make_sse_generator(chat_stream_fn, messages, body, request.app.state.http_client)
    return _streaming_response(generator)


@app.post("/infer/rule", response_model=InferResponse)
@limiter.limit("10/minute")
async def infer_rule(request: Request, body: InferRequest) -> InferResponse:
    """
    병원 규칙 Q&A LLM 추론
    의사·간호사가 병원 내부 규칙(당직, 물품, 위생 등)에 대해 질의할 때 사용
    """
    _start = _time.time()
    query_preview = body.query[:50] + "..." if len(body.query) > 50 else body.query
    logger.info("Rule infer request: query=%s", repr(query_preview))

    corrected_query = await correct_typos_async(body.query)
    if corrected_query != body.query:
        logger.info("Rule query typo corrected: '%s' -> '%s'", body.query, corrected_query)

    # RAG: ChromaDB + MySQL 하이브리드 검색으로 병원규칙 컨텍스트 주입
    rule_context = ""
    try:
        from rule_context_service import build_rule_context
        rule_context = await build_rule_context(corrected_query)
    except Exception as exc:
        logger.warning("Rule context build skipped: %s", exc)
    logger.info("Rule context: %d chars", len(rule_context))

    # 검색 결과가 없어도 LLM을 통해 자연스러운 응답 생성
    if not rule_context:
        rule_context = (
            "[참고: 병원 규칙 검색 결과]\n"
            "검색된 규칙이 없습니다. 사용자의 질문에 대해 일반적인 안내를 제공하되, "
            "정확한 내용은 관리자에게 확인하도록 안내해 주세요."
        )
        logger.info("Rule infer: no context found, using fallback context for LLM")

    messages = _build_rule_messages(corrected_query, rule_context, body.history)

    try:
        raw_text = await _chat_llm(messages, body, request.app.state.http_client)
        generated_text = clean_llm_response(raw_text)
    except Exception:
        _elapsed = (_time.time() - _start) * 1000
        metrics.record_request(latency_ms=_elapsed, success=False, vector_hit=len(rule_context) > 0)
        raise

    _elapsed = (_time.time() - _start) * 1000
    metrics.record_request(latency_ms=_elapsed, success=True, vector_hit=len(rule_context) > 0)
    logger.info("Rule infer response: length=%d", len(generated_text))
    return InferResponse(generated_text=generated_text)


@app.post("/infer/rule/stream")
@limiter.limit("10/minute")
async def infer_rule_stream(request: Request, body: InferRequest):
    """
    병원 규칙 Q&A 스트리밍 추론 (SSE)
    /infer/rule과 동일한 로직, stream:true로 토큰 단위 전송
    """
    query_preview = body.query[:50] + "..." if len(body.query) > 50 else body.query
    logger.info("Rule stream request: query=%s", repr(query_preview))

    corrected_query = await correct_typos_async(body.query)

    # RAG: 병원규칙 컨텍스트 주입
    rule_context = ""
    try:
        from rule_context_service import build_rule_context
        rule_context = await build_rule_context(corrected_query)
    except Exception as exc:
        logger.warning("Rule context build skipped (stream): %s", exc)
    logger.info("Rule context (stream): %d chars", len(rule_context))

    # 검색 결과가 없어도 LLM을 통해 자연스러운 응답 생성
    if not rule_context:
        rule_context = (
            "[참고: 병원 규칙 검색 결과]\n"
            "검색된 규칙이 없습니다. 사용자의 질문에 대해 일반적인 안내를 제공하되, "
            "정확한 내용은 관리자에게 확인하도록 안내해 주세요."
        )
        logger.info("Rule stream: no context found, using fallback context for LLM")

    messages = _build_rule_messages(corrected_query, rule_context, body.history)
    chat_stream_fn = _get_chat_stream_fn()
    generator = _make_sse_generator(chat_stream_fn, messages, body, request.app.state.http_client)
    return _streaming_response(generator)


@app.post("/feedback", response_model=FeedbackResponse)
@limiter.limit("10/minute")
async def submit_feedback(request: Request, body: FeedbackRequest) -> FeedbackResponse:
    """
    LLM 응답 품질 피드백 저장
    """
    logger.info("Feedback received: score=%d, endpoint=%s", body.score, body.endpoint)

    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    """
                    INSERT INTO llm_feedback (session_id, query, response, score, comment, endpoint)
                    VALUES (%s, %s, %s, %s, %s, %s)
                    """,
                    (body.session_id, body.query, body.response,
                     body.score, body.comment, body.endpoint),
                )
            await conn.commit()
        return FeedbackResponse()
    except Exception as exc:
        logger.error("Failed to save feedback: %s", exc)
        raise HTTPException(status_code=500, detail="피드백 저장에 실패했습니다")


@app.get("/feedback/stats")
async def feedback_stats():
    """피드백 통계 조회"""
    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                await cur.execute("""
                    SELECT
                        endpoint,
                        COUNT(*) as total,
                        ROUND(AVG(score), 2) as avg_score,
                        SUM(CASE WHEN score <= 2 THEN 1 ELSE 0 END) as low_count,
                        SUM(CASE WHEN score >= 4 THEN 1 ELSE 0 END) as high_count
                    FROM llm_feedback
                    GROUP BY endpoint
                """)
                stats = await cur.fetchall()
        return {"status": "ok", "stats": stats}
    except Exception as exc:
        logger.error("Failed to get feedback stats: %s", exc)
        raise HTTPException(status_code=500, detail="통계 조회에 실패했습니다")


# ════════════════════════════════════════════════════════════════════════════
# 규칙 인덱싱 API (Spring Admin → Python LLM)
# ════════════════════════════════════════════════════════════════════════════

@app.post("/index/rule", response_model=RuleIndexResponse)
async def index_rule(request: Request, body: RuleIndexRequest) -> RuleIndexResponse:
    """
    병원규칙 단건 인덱싱: medical_rule 테이블 upsert + ChromaDB 벡터 인덱싱
    Spring AdminRuleService에서 규칙 생성/수정 시 호출
    """
    logger.info("Rule index request: rule_id=%d, title=%s", body.rule_id, body.title)

    try:
        pool = await get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                # medical_rule 테이블에 upsert (hospital_rule.id를 source_id로 매핑)
                await cur.execute(
                    "SELECT id FROM medical_rule WHERE source_id = %s",
                    (body.rule_id,),
                )
                existing = await cur.fetchone()

                if existing:
                    await cur.execute(
                        """
                        UPDATE medical_rule
                        SET category = %s, title = %s, content = %s, target = %s
                        WHERE source_id = %s
                        """,
                        (body.category, body.title, body.content, body.target or "", body.rule_id),
                    )
                    medical_rule_id = existing["id"]
                    logger.info("Rule updated in medical_rule: source_id=%d, id=%d", body.rule_id, medical_rule_id)
                else:
                    await cur.execute(
                        """
                        INSERT INTO medical_rule (category, title, content, target, source_id, created_at)
                        VALUES (%s, %s, %s, %s, %s, NOW())
                        """,
                        (body.category, body.title, body.content, body.target or "", body.rule_id),
                    )
                    medical_rule_id = cur.lastrowid
                    logger.info("Rule inserted to medical_rule: source_id=%d, id=%d", body.rule_id, medical_rule_id)

            await conn.commit()

        # ChromaDB 벡터 인덱싱
        if body.active:
            from embedding_service import get_embedding
            from rule_context_service import add_rule_documents

            text = f"카테고리: {body.category}\n규칙명: {body.title}\n내용: {body.content}"
            embedding = await get_embedding(text)
            add_rule_documents(
                ids=[f"rule_{medical_rule_id}"],
                documents=[text],
                embeddings=[embedding],
                metadatas=[{
                    "type": "rule",
                    "category": body.category,
                    "title": body.title,
                    "target": body.target or "",
                    "source_id": str(body.rule_id),
                }],
            )
            logger.info("Rule indexed to ChromaDB: rule_%d", medical_rule_id)

        return RuleIndexResponse(rule_id=body.rule_id, message="규칙이 인덱싱되었습니다")

    except Exception as exc:
        logger.error("Rule indexing failed: %s: %s", type(exc).__name__, exc)
        raise HTTPException(status_code=500, detail=f"규칙 인덱싱 실패: {exc}")


@app.delete("/index/rule/{rule_id}", response_model=RuleIndexResponse)
async def delete_rule_index(request: Request, rule_id: int) -> RuleIndexResponse:
    """
    병원규칙 인덱스 삭제: medical_rule 삭제 + ChromaDB 벡터 삭제
    Spring AdminRuleService에서 규칙 삭제 시 호출
    """
    logger.info("Rule delete index request: rule_id=%d", rule_id)

    try:
        pool = await get_pool()
        medical_rule_id = None

        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                await cur.execute(
                    "SELECT id FROM medical_rule WHERE source_id = %s",
                    (rule_id,),
                )
                existing = await cur.fetchone()
                if existing:
                    medical_rule_id = existing["id"]
                    await cur.execute("DELETE FROM medical_rule WHERE source_id = %s", (rule_id,))
                    logger.info("Rule deleted from medical_rule: source_id=%d", rule_id)
            await conn.commit()

        # ChromaDB에서 삭제
        if medical_rule_id:
            try:
                from rule_context_service import _get_collection
                col = _get_collection()
                col.delete(ids=[f"rule_{medical_rule_id}"])
                logger.info("Rule deleted from ChromaDB: rule_%d", medical_rule_id)
            except Exception as vec_exc:
                logger.warning("ChromaDB delete failed (non-critical): %s", vec_exc)

        return RuleIndexResponse(rule_id=rule_id, message="규칙 인덱스가 삭제되었습니다")

    except Exception as exc:
        logger.error("Rule index delete failed: %s: %s", type(exc).__name__, exc)
        raise HTTPException(status_code=500, detail=f"규칙 인덱스 삭제 실패: {exc}")


if __name__ == "__main__":
    import uvicorn

    settings = get_settings()
    uvicorn.run(app, host=settings.host, port=settings.port)
