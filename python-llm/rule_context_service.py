"""
병원규칙 RAG 벡터 검색 -> LLM 컨텍스트 주입 서비스
medical_rules 전용 컬렉션 사용
하이브리드 검색: ChromaDB 벡터 + MySQL 키워드 병행, 결과 병합
"""

import asyncio
import logging
import re

import aiomysql

from config import get_settings

logger = logging.getLogger(__name__)

# 벡터 검색 distance 임계값 (cosine space: 0=동일, 2=정반대)
# 0.5 이상이면 관련성 낮은 결과로 판단하여 제외
VECTOR_DISTANCE_THRESHOLD = 0.5

# 카테고리 키워드 매핑 (사용자 입력 → DB category LIKE 패턴 리스트)
# 관련 카테고리를 모두 포함하여 누락 방지
_CATEGORY_MAP: dict[str, list[str]] = {
    "위생": ["위생/감염", "원내감염"],
    "감염": ["위생/감염", "원내감염"],
    "소독": ["위생/감염"],
    "격리": ["위생/감염", "원내감염"],
    "결핵": ["위생/감염", "원내감염"],
    "당직": ["당직/근무"],
    "근무": ["당직/근무"],
    "교대": ["당직/근무"],
    "물품": ["물품/비품"],
    "비품": ["물품/비품"],
    "약품": ["물품/비품"],
    "응급": ["응급"],
    "수술": ["수술"],
    "입원": ["입원/퇴원"],
    "퇴원": ["입원/퇴원"],
    "안전": ["안전"],
    "보안": ["안전"],
    "환자권리": ["환자권리"],
    "환자안전": ["환자안전"],
    "투약": ["투약/처방"],
    "처방": ["투약/처방"],
    "검사": ["검사/진단"],
    "진단": ["검사/진단"],
}

# 검색 노이즈 단어 (키워드 추출 시 제거) — 어미 포함 형태도 매칭
_NOISE_WORDS = {"목록", "리스트", "알려줘", "알려주세요", "뭐가", "있어",
                "있나요", "어떤", "전부", "모두", "전체", "규칙", "규정",
                "내용", "정보", "관련", "대해", "에대해", "문제",
                "대하여", "대해서", "에대하여", "에대해서", "어떻게", "무엇",
                "알고", "싶어요", "궁금", "질문", "말해줘", "설명", "해주세요"}
# "규칙은", "규칙을" 등 조사 붙은 형태도 노이즈로 처리
_NOISE_STEMS = ["규칙", "규정", "목록", "리스트", "내용", "정보", "관련", "문제"]

# 전체/목록 요청 감지 키워드
_BROAD_QUERY_WORDS = {"전체", "전부", "모두", "목록", "리스트", "종류", "어떤것", "뭐가"}

# 한국어 조사 패턴 (키워드에서 제거)
_PARTICLE_RE = re.compile(r"(에서|에게|부터|까지|으로|에|은|는|이|가|을|를|의|로|와|과|도|만)$")


def _strip_particle(word: str) -> str:
    """한국어 조사 제거 (2글자 이상 유지)"""
    result = _PARTICLE_RE.sub("", word)
    return result if len(result) >= 2 else word


def _is_noise_word(word: str) -> bool:
    """노이즈 단어 여부 판단 (어미/조사 포함 형태도 매칭)"""
    if word in _NOISE_WORDS:
        return True
    for stem in _NOISE_STEMS:
        if word.startswith(stem):
            return True
    return False


def _extract_keywords(query: str) -> list[str]:
    """질문에서 2글자 이상 한국어/영어 키워드 추출 (조사 제거, 노이즈 제거, 최대 8개)"""
    words = re.findall(r"[가-힣a-zA-Z]{2,}", query)
    # 조사 제거 + 중복 제거
    seen = set()
    stripped = []
    for w in words:
        s = _strip_particle(w)
        if s not in seen:
            seen.add(s)
            stripped.append(s)
    filtered = [w for w in stripped if not _is_noise_word(w)]
    return filtered[:8] if filtered else stripped[:8]


def _is_broad_query(query: str) -> bool:
    """전체/목록/종류 등 광범위 요청인지 판단"""
    words = set(re.findall(r"[가-힣a-zA-Z]{2,}", query))
    stripped = {_strip_particle(w) for w in words}
    # 광범위 키워드가 포함되고, 카테고리 키워드가 없으면 전체 조회
    has_broad = bool(stripped & _BROAD_QUERY_WORDS)
    has_category = any(kw in query for kw in _CATEGORY_MAP)
    return has_broad and not has_category


async def search_all_rules(limit: int = 30) -> list[dict]:
    """전체 규칙 조회 (카테고리별 정렬)"""
    try:
        pool = await _get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                await cur.execute(
                    """
                    SELECT id, category, title, content, target
                    FROM medical_rule
                    ORDER BY category, id
                    LIMIT %s
                    """,
                    (limit,),
                )
                rows = await cur.fetchall()
        logger.info("All rules search: %d results", len(rows))
        return rows
    except Exception as exc:
        logger.warning("All rules search failed: %s: %s", type(exc).__name__, exc)
        return []


def _detect_categories(query: str) -> list[str]:
    """쿼리에서 카테고리 키워드를 감지하여 관련 DB category 값 리스트 반환"""
    categories = []
    for keyword, cat_list in _CATEGORY_MAP.items():
        if keyword in query:
            for cat in cat_list:
                if cat not in categories:
                    categories.append(cat)
    return categories


async def _get_pool() -> aiomysql.Pool:
    """MySQL 커넥션 풀 (medical_context_service와 동일)"""
    from medical_context_service import get_pool
    return await get_pool()


async def search_medical_rule_mysql(query: str, limit: int = 5) -> list[dict]:
    """
    MySQL medical_rule 테이블에서 병원규칙 검색
    LIKE 검색 사용, 키워드 기반
    """
    try:
        pool = await _get_pool()
        keywords = _extract_keywords(query)

        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                if keywords:
                    # LIKE 검색: content 또는 title에 키워드 포함
                    conditions = []
                    params = []
                    for kw in keywords:
                        like_val = f"%{kw}%"
                        conditions.append("(content LIKE %s OR title LIKE %s OR category LIKE %s)")
                        params.extend([like_val, like_val, like_val])
                    params.append(limit)
                    sql = f"""
                        SELECT id, category, title, content, target
                        FROM medical_rule
                        WHERE {" OR ".join(conditions)}
                        ORDER BY id DESC
                        LIMIT %s
                    """
                    await cur.execute(sql, params)
                else:
                    # 키워드 없으면 최근 규칙 조회
                    search_term = query[:30] if query else ""
                    if search_term:
                        await cur.execute(
                            """
                            SELECT id, category, title, content, target
                            FROM medical_rule
                            WHERE content LIKE %s OR title LIKE %s
                            ORDER BY id DESC
                            LIMIT %s
                            """,
                            (f"%{search_term}%", f"%{search_term}%", limit),
                        )
                    else:
                        await cur.execute(
                            """
                            SELECT id, category, title, content, target
                            FROM medical_rule
                            ORDER BY id DESC
                            LIMIT %s
                            """,
                            (limit,),
                        )
                rows = await cur.fetchall()

        if rows:
            logger.info("Rule MySQL search: %d results (query: %s...)", len(rows), query[:30])
        return rows
    except Exception as exc:
        logger.warning("Rule MySQL search failed: %s: %s", type(exc).__name__, exc)
        return []


async def search_medical_rule_by_categories(categories: list[str], limit: int = 15) -> list[dict]:
    """여러 카테고리에 해당하는 규칙을 MySQL에서 조회"""
    if not categories:
        return []
    try:
        pool = await _get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                conditions = []
                params = []
                for cat in categories:
                    conditions.append("category LIKE %s")
                    params.append(f"%{cat}%")
                params.append(limit)
                sql = f"""
                    SELECT id, category, title, content, target
                    FROM medical_rule
                    WHERE {" OR ".join(conditions)}
                    ORDER BY category, id
                    LIMIT %s
                """
                await cur.execute(sql, params)
                rows = await cur.fetchall()
        if rows:
            logger.info("Rule category search: %d results (categories: %s)", len(rows), categories)
        return rows
    except Exception as exc:
        logger.warning("Rule category search failed: %s: %s", type(exc).__name__, exc)
        return []


def _get_collection():
    """병원규칙 전용 컬렉션 사용"""
    from vector_store import get_rule_collection
    return get_rule_collection()


def get_rule_document_count() -> int:
    """병원규칙 벡터 저장소 문서 수 조회"""
    try:
        col = _get_collection()
        return col.count()
    except Exception:
        return 0


def add_rule_documents(
    ids: list[str],
    documents: list[str],
    embeddings: list[list[float]],
    metadatas: list[dict] | None = None,
):
    """병원규칙을 전용 컬렉션에 추가"""
    col = _get_collection()
    col.upsert(ids=ids, documents=documents, embeddings=embeddings, metadatas=metadatas)
    logger.info("Added/updated %d rule documents to rule collection", len(ids))


async def search_rule_vector_store(query: str, top_k: int = 3) -> list[dict]:
    """ChromaDB 벡터 검색으로 관련 병원규칙 조회 (distance 임계값 적용)"""
    settings = get_settings()
    if not settings.use_vector_search:
        return []

    try:
        from embedding_service import get_embedding

        col = _get_collection()
        doc_count = await asyncio.to_thread(col.count)
        if doc_count == 0:
            return []

        query_embedding = await get_embedding(query)
        results = await asyncio.to_thread(
            col.query,
            query_embeddings=[query_embedding],
            n_results=min(top_k, doc_count),
        )

        items = []
        if results and results["documents"]:
            for i, doc in enumerate(results["documents"][0]):
                distance = results["distances"][0][i] if results["distances"] else 0
                # distance 임계값 필터링: 관련성 낮은 결과 제외
                if distance > VECTOR_DISTANCE_THRESHOLD:
                    logger.debug("Rule vector result filtered (distance=%.3f > %.3f): %s...",
                                 distance, VECTOR_DISTANCE_THRESHOLD, doc[:50])
                    continue
                item = {
                    "document": doc,
                    "metadata": results["metadatas"][0][i] if results["metadatas"] else {},
                    "distance": distance,
                }
                items.append(item)

        logger.info("Rule vector search: %d results after filtering (query: %s...)",
                     len(items), query[:30])
        return items
    except Exception as exc:
        logger.warning("Rule vector search failed: %s: %s", type(exc).__name__, exc)
        return []


def _format_rule_item(item: dict, is_vector: bool) -> list[str]:
    """벡터/MySQL 검색 결과를 컨텍스트 형식으로 변환"""
    parts = []
    if is_vector:
        meta = item.get("metadata", {})
        category = meta.get("category", "")
        title = meta.get("title", "")
        target = meta.get("target", "")
        doc = item.get("document", "")[:800]
    else:
        category = item.get("category", "")
        title = item.get("title", "")
        target = item.get("target", "")
        content = item.get("content", "")
        doc = f"카테고리: {category}\n규칙명: {title}\n내용: {content}"[:800]
    if category:
        parts.append(f"카테고리: {category}")
    if title:
        parts.append(f"규칙명: {title}")
    if target:
        parts.append(f"적용 대상: {target}")
    parts.append(doc)
    parts.append("")
    return parts


def _deduplicate_results(vector_items: list[dict], mysql_rows: list[dict]) -> tuple[list[dict], list[dict]]:
    """벡터/MySQL 결과 중복 제거 (title 기준)"""
    seen_titles = set()
    deduped_vector = []
    for item in vector_items:
        title = item.get("metadata", {}).get("title", "")
        if title and title not in seen_titles:
            seen_titles.add(title)
            deduped_vector.append(item)
        elif not title:
            deduped_vector.append(item)

    deduped_mysql = []
    for row in mysql_rows:
        title = row.get("title", "")
        if title and title not in seen_titles:
            seen_titles.add(title)
            deduped_mysql.append(row)
        elif not title:
            deduped_mysql.append(row)

    return deduped_vector, deduped_mysql


def _is_followup_query(query: str) -> bool:
    """후속 질문인지 판단 (대명사, 짧은 질문, 추가 요청 패턴)"""
    followup_patterns = [
        "그거", "그것", "그건", "거기", "이거", "저거",
        "더 자세히", "자세하게", "더 알려", "추가로", "보충",
        "다른 건", "다른건", "또 뭐", "또 있", "그 외",
        "아까", "방금", "위에서", "말한", "언급한",
    ]
    stripped = query.strip()
    # 짧은 질문 (10자 이하)이면 후속 질문 가능성 높음
    if len(stripped) <= 10:
        return True
    for pattern in followup_patterns:
        if pattern in stripped:
            return True
    return False


def _expand_query_from_history(query: str, history: list | None) -> str:
    """
    후속 질문일 때 이전 대화에서 키워드를 추출하여 RAG 검색 쿼리를 확장.
    history 형식: [{"role": "user"|"assistant", "content": "..."}]
    """
    if not history:
        return query
    if not _is_followup_query(query):
        return query

    # 이전 user 메시지에서 키워드 추출 (최근 2턴)
    prev_keywords = []
    user_msgs = [m for m in history if (m.get("role") or getattr(m, "role", None)) == "user"]
    for msg in user_msgs[-2:]:
        content = msg.get("content") or getattr(msg, "content", "")
        prev_keywords.extend(_extract_keywords(content))

    if not prev_keywords:
        return query

    # 현재 쿼리에 이미 포함된 키워드 제외
    current_keywords = set(_extract_keywords(query))
    new_keywords = [kw for kw in prev_keywords if kw not in current_keywords]

    if not new_keywords:
        return query

    # 최대 4개 키워드로 확장
    expanded = query + " " + " ".join(new_keywords[:4])
    logger.info("Query expanded for followup: '%s' -> '%s'", query, expanded)
    return expanded


async def build_rule_context(query: str, history: list | None = None) -> str:
    """
    사용자 질문에 대한 병원규칙 컨텍스트 빌드

    검색 전략:
    0. 전체/목록 요청 시 → 전체 규칙 조회 (최대 30건)
    1. 카테고리 감지 시 → 카테고리 기반 MySQL 조회 (최대 15건)
       + 벡터 검색 병행하여 보충
    2. 카테고리 미감지 → 벡터 + MySQL 하이브리드 병행 검색
    """
    try:
        # 후속 질문이면 이전 대화에서 키워드를 추출하여 RAG 검색 쿼리 확장
        search_query = _expand_query_from_history(query, history)

        settings = get_settings()
        parts = []
        top_k = settings.vector_search_top_k

        # 전체/목록 요청 감지
        if _is_broad_query(search_query):
            logger.info("Broad query detected: returning all rules")
            all_rows = await search_all_rules(limit=30)
            for row in all_rows:
                parts.extend(_format_rule_item(row, is_vector=False))
            if parts:
                parts.insert(0, "[참고: 병원 규칙 검색 결과 — 전체 목록]")
                parts.insert(1, "")
            context = "\n".join(parts) if parts else ""
            max_chars = max(settings.medical_context_max_chars, 6000)
            if len(context) > max_chars:
                truncated = context[:max_chars]
                last_newline = truncated.rfind("\n")
                context = truncated[:last_newline] if last_newline > 0 else truncated
            return context

        # 카테고리 감지
        detected_categories = _detect_categories(search_query)

        if detected_categories:
            # 카테고리 감지됨: 카테고리 기반 검색 + 벡터 검색 병행
            logger.info("Category detected: %s", detected_categories)
            cat_task = search_medical_rule_by_categories(detected_categories, limit=15)
            vector_task = search_rule_vector_store(search_query, top_k=top_k)
            category_rows, vector_results = await asyncio.gather(cat_task, vector_task)

            # 카테고리 결과 먼저, 벡터 결과로 보충 (중복 제거)
            seen_titles = set()
            for row in category_rows:
                title = row.get("title", "")
                if title not in seen_titles:
                    seen_titles.add(title)
                    parts.extend(_format_rule_item(row, is_vector=False))
            for item in vector_results:
                title = item.get("metadata", {}).get("title", "")
                if title and title not in seen_titles:
                    seen_titles.add(title)
                    parts.extend(_format_rule_item(item, is_vector=True))
        else:
            # 카테고리 미감지: 벡터 + MySQL 하이브리드 병행 검색
            vector_task = search_rule_vector_store(search_query, top_k=top_k)
            mysql_task = search_medical_rule_mysql(search_query, limit=top_k)
            vector_results, mysql_rows = await asyncio.gather(vector_task, mysql_task)

            # 중복 제거 후 병합 (벡터 결과 우선)
            deduped_vector, deduped_mysql = _deduplicate_results(vector_results, mysql_rows)

            for item in deduped_vector:
                parts.extend(_format_rule_item(item, is_vector=True))
            for row in deduped_mysql:
                parts.extend(_format_rule_item(row, is_vector=False))

        if parts:
            parts.insert(0, "[참고: 병원 규칙 검색 결과]")
            parts.insert(1, "")

        context = "\n".join(parts) if parts else ""

        # 카테고리 검색은 결과가 많으므로 컨텍스트 크기를 넉넉하게
        max_chars = settings.medical_context_max_chars
        if detected_categories:
            max_chars = max(max_chars, 4000)
        if len(context) > max_chars:
            truncated = context[:max_chars]
            last_newline = truncated.rfind("\n")
            if last_newline > 0:
                context = truncated[:last_newline]
            else:
                context = truncated

        return context
    except Exception as exc:
        logger.warning("build_rule_context failed: %s: %s", type(exc).__name__, exc)
        return ""
