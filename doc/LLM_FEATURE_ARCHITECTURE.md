# LLM 추론 기능 아키텍처 문서

## 개요

HMS(Hospital Management System)는 두 가지 LLM 추론 기능을 제공합니다.

| 기능                    | 대상 사용자             | 접근 경로                              | 인증   |
| ----------------------- | ----------------------- | -------------------------------------- | ------ |
| 증상 분석 → 진료과 추천 | 비회원 환자             | `/reservation/symptom-reservation`     | 불필요 |
| 병원 규칙 Q&A 챗봇      | 내부 직원 (의사/간호사) | `/doctor/chatbot`, `/nurse/chatbot` 등 | 필요   |

---

## 1. 증상 분석 → 진료과 추천

### 1.1 기능 목적

환자가 자신의 증상을 자유 텍스트로 입력하면, LLM이 증상을 분석하여 적합한 진료과를 추천하고 그 이유를 설명합니다. 추천된 진료과의 전문의 목록과 예약 가능 시간을 바로 제공하여 예약까지 원스톱으로 진행할 수 있습니다.

### 1.2 작업 이유

- 환자가 어떤 진료과를 방문해야 하는지 모르는 경우가 빈번
- 기존 직접 선택 예약은 진료과 지식이 전제되어 접근성이 낮음
- LLM 기반 추천으로 환자 경험 개선 및 적절한 진료과 배정률 향상

### 1.3 동작 구조

```
[브라우저]                    [Spring Boot]                    [Python LLM]
    │                              │                               │
    │ POST /llm/symptom/analyze    │                               │
    │ { symptomText: "..." }       │                               │
    │─────────────────────────────>│                               │
    │                              │  DB: 활성 진료과 목록 조회     │
    │                              │  (5분 TTL 캐시)               │
    │                              │                               │
    │                              │  POST /infer/medical           │
    │                              │  { query: 프롬프트+증상,       │
    │                              │    max_length: 256 }           │
    │                              │──────────────────────────────>│
    │                              │                               │
    │                              │                    ┌──────────┤
    │                              │                    │ RAG 검색  │
    │                              │                    │ (MySQL +  │
    │                              │                    │ ChromaDB) │
    │                              │                    └──────────┤
    │                              │                               │
    │                              │                    ┌──────────┤
    │                              │                    │ vLLM/    │
    │                              │                    │ Ollama   │
    │                              │                    │ 추론     │
    │                              │                    └──────────┤
    │                              │                               │
    │                              │  { generated_text: "..." }    │
    │                              │<──────────────────────────────│
    │                              │                               │
    │                              │  진료과명 추출 (정규식+탐색)   │
    │                              │  추천 이유 추출               │
    │                              │  DB에서 진료과 ID 매칭        │
    │                              │                               │
    │  { departmentId, name,       │                               │
    │    reason }                   │                               │
    │<─────────────────────────────│                               │
    │                              │                               │
    │ GET /api/reservation/doctors  │                               │
    │ (추천 진료과의 전문의 조회)    │                               │
    │─────────────────────────────>│                               │
    │                              │                               │
    │  [ {id, name, availableDays} ]│                               │
    │<─────────────────────────────│                               │
```

### 1.4 주요 컴포넌트

| 계층       | 파일                           | 역할                                             |
| ---------- | ------------------------------ | ------------------------------------------------ |
| Controller | `SymptomController.java`       | `POST /llm/symptom/analyze` — 비인증 REST API    |
| Service    | `SymptomAnalysisService.java`  | 프롬프트 생성, LLM 호출, 응답 파싱, 진료과 매칭  |
| DTO        | `SymptomRequest.java`          | 요청: `{ symptomText }`                          |
| DTO        | `SymptomResponse.java`         | 응답: `{ departmentId, departmentName, reason }` |
| Frontend   | `symptom-reservation.mustache` | 증상 입력 → 추천 결과 → 예약 폼 (AJAX)           |
| Python     | `app.py` `/infer/medical`      | RAG 컨텍스트 주입 + LLM 추론                     |
| Python     | `prompts/medical_system.txt`   | 시스템 프롬프트 (진료과 추천 형식 지정)          |
| Python     | `medical_context_service.py`   | MySQL + ChromaDB 하이브리드 검색                 |

### 1.5 프롬프트 설계

```
당신은 환자의 증상을 분석하여 적합한 진료과를 추천하는 의료 안내 AI입니다.
다음 환자의 증상을 분석하고, 가장 적합한 진료과를 하나만 추천하세요.

[선택 가능한 진료과]
내과|외과|소아과|이비인후과|정형외과|피부과|안과|산부인과   ← DB에서 동적 로드

반드시 아래 형식으로 답변하세요:
진료과: (위 진료과 중 하나)
이유: (환자의 증상과 추천 진료과의 연관성을 2~3문장으로 설명)

환자 증상: {사용자 입력}
```

- 진료과 목록은 DB에서 동적 로드 (5분 TTL 캐시)
- 관리자가 진료과를 추가/삭제하면 최대 5분 후 자동 반영

### 1.6 응답 파싱

LLM 응답에서 두 가지를 추출합니다:

1. **진료과명** — 정규식 `진료과:\s*(.+)` → 알려진 진료과명 매칭 → 텍스트 내 직접 탐색 → 기본값 "내과"
2. **추천 이유** — 정규식 `이유:\s*(.+)` → 줄바꿈 전까지

### 1.7 보안 설정

- CSRF 제외: `/llm/symptom/**` (비인증 AJAX)
- Rate Limit: IP당 20회/분
- 접근 권한: `permitAll()` (비회원 허용)

---

## 2. 병원 규칙 Q&A 챗봇

### 2.1 기능 목적

병원 내부 직원(의사, 간호사)이 당직/근무, 물품/비품, 위생/감염, 응급 처치 등 병원 내부 규칙에 대해 자연어로 질문하면, RAG(Retrieval-Augmented Generation) 기반으로 등록된 규칙을 검색하여 LLM이 자연스러운 대화체로 답변합니다.

### 2.2 작업 이유

- 병원 규칙이 문서/게시판에 분산되어 있어 직원이 필요한 규칙을 찾기 어려움
- 규칙 개수가 증가할수록 검색 어려움 가중
- LLM + RAG로 자연어 질의 → 관련 규칙 자동 검색 → 대화체 답변 제공
- 관리자가 등록한 규칙이 자동으로 챗봇에 반영되는 파이프라인 구축

### 2.3 동작 구조

```
[브라우저]                    [Spring Boot]                    [Python LLM]
    │                              │                               │
    │ POST /llm/chatbot/query      │                               │
    │ { query, history[] }         │                               │
    │─────────────────────────────>│                               │
    │                              │                               │
    │                              │  POST /infer/rule              │
    │                              │  { query, history[],           │
    │                              │    max_length: 1024 }          │
    │                              │──────────────────────────────>│
    │                              │                               │
    │                              │                    ┌──────────┤
    │                              │                    │ 1. 오타   │
    │                              │                    │    교정   │
    │                              │                    └──────────┤
    │                              │                    ┌──────────┤
    │                              │                    │ 2. RAG   │
    │                              │                    │ 검색     │
    │                              │                    │ - 카테고리│
    │                              │                    │   감지   │
    │                              │                    │ - MySQL  │
    │                              │                    │   LIKE   │
    │                              │                    │ - Chroma │
    │                              │                    │   벡터   │
    │                              │                    └──────────┤
    │                              │                    ┌──────────┤
    │                              │                    │ 3. LLM   │
    │                              │                    │ 추론     │
    │                              │                    │ (컨텍스트│
    │                              │                    │  + 질문) │
    │                              │                    └──────────┤
    │                              │                               │
    │                              │  { generated_text: "..." }    │
    │                              │<──────────────────────────────│
    │                              │                               │
    │                              │  ChatbotHistory 저장           │
    │                              │                               │
    │  "당직 규칙에 대해 안내..."   │                               │
    │<─────────────────────────────│                               │
```

### 2.4 주요 컴포넌트

| 계층       | 파일                      | 역할                                                       |
| ---------- | ------------------------- | ---------------------------------------------------------- |
| Controller | `ChatController.java`     | `POST /llm/chatbot/query` — 인증 필요 REST API             |
| Service    | `ChatService.java`        | Python LLM 호출 + 대화 히스토리 저장                       |
| Entity     | `ChatbotHistory.java`     | 질의/응답 이력 저장                                        |
| Frontend   | 각 역할별 chatbot 페이지  | 의사/간호사/접수/관리자 챗봇 UI                            |
| Python     | `app.py` `/infer/rule`    | 오타 교정 + RAG + LLM 추론                                 |
| Python     | `prompts/rule_system.txt` | 시스템 프롬프트 (자연스러운 대화체)                        |
| Python     | `rule_context_service.py` | 카테고리 감지 + MySQL LIKE + ChromaDB 벡터 하이브리드 검색 |

### 2.5 RAG 검색 전략

```
사용자 질문
    │
    ├─ 카테고리 감지 ("위생", "당직" 등 키워드)
    │   ├─ 감지됨 → 카테고리 기반 MySQL 조회 (최대 15건) + 벡터 검색 보충
    │   └─ 미감지 → 하이브리드 병행 검색
    │
    ├─ MySQL LIKE 검색
    │   └─ 키워드 추출 (조사 제거, 노이즈 필터) → content/title/category LIKE 매칭
    │
    └─ ChromaDB 벡터 검색
        └─ Ollama 임베딩(nomic-embed-text) → cosine distance (임계값 0.5)
```

**키워드 추출 로직:**

1. 한국어 조사 제거: "규정에" → "규정", "위생은" → "위생"
2. 노이즈 단어 필터: "대하여", "알려줘", "목록" 등 제거
3. 남은 키워드로 검색, 전부 노이즈면 원본 단어로 폴백

### 2.6 프롬프트 설계

```
당신은 병원 내부 규칙에 대해 직원의 질문에 친절하고 자연스럽게 답변하는 AI 어시스턴트입니다.

답변 스타일:
- 동료에게 설명하듯 자연스러운 대화체로 답변하세요.
- 핵심 내용을 먼저 간결하게 요약하고, 필요 시 세부 사항을 덧붙이세요.
- 규칙의 카테고리, 규칙명, 적용 대상은 답변 흐름 속에 자연스럽게 녹여서 전달하세요.

답변 규칙:
1. 제공된 자료를 기반으로 자연스럽게 풀어서 설명하세요.
2. 자료에 없으면 일반적인 안내 후 "관리자에게 확인해 주세요" 안내.
3. 의료 진단 질문은 의학 상담 챗봇 안내.
```

### 2.7 규칙 인덱싱 파이프라인

관리자가 `/admin/rule/new`에서 규칙을 등록하면 챗봇 RAG에 자동 반영됩니다.

```
[Admin 규칙 등록/수정]          [Spring]                    [Python LLM]
    │                              │                               │
    │ POST /admin/rule/new         │                               │
    │─────────────────────────────>│                               │
    │                              │ hospital_rule 테이블 저장      │
    │                              │                               │
    │                              │ POST /index/rule (비동기)      │
    │                              │ { rule_id, title, content,     │
    │                              │   category, target, active }   │
    │                              │──────────────────────────────>│
    │                              │                               │
    │                              │              medical_rule upsert│
    │                              │              ChromaDB 벡터 인덱싱│
    │                              │                               │
    │                              │              챗봇 RAG 검색 반영 │
```

**전체 인덱싱**: `/admin/rule/list` 진입 시 `@Async`로 전체 활성 규칙 일괄 인덱싱 (기존 데이터 반영)

**삭제**: `DELETE /index/rule/{id}` → `medical_rule` 삭제 + ChromaDB 벡터 삭제

### 2.8 멀티턴 대화 지원

브라우저에서 최근 3턴(6메시지)의 대화 이력을 `history[]`로 전달하여 자연스러운 후속 질의응답을 지원합니다.

```
사용자: "당직 규정 알려줘"          ← 1턴
AI: "당직 규정은 ..."
사용자: "그거 더 자세히 알려줘"      ← 후속 질문 (대명사)
    │
    ├─ 후속 질문 감지 (_is_followup_query)
    │   └─ "그거", 짧은 질문, 대명사 패턴 매칭
    │
    ├─ 쿼리 확장 (_expand_query_from_history)
    │   └─ 이전 user 메시지에서 키워드 추출 → "그거 더 자세히 알려줘 당직"
    │
    ├─ 확장된 쿼리로 RAG 검색 (카테고리/벡터/MySQL)
    │
    └─ LLM 추론: system + context + history(3턴) + user
        └─ 이전 대화 맥락을 참고하여 중복 없이 보충 답변
```

**데이터 흐름:**
- 프론트엔드: `chatHistory[]` 배열로 클라이언트 측 관리, 최근 6개 메시지 전송
- Spring Boot: non-stream/stream 모두 `history`를 Python에 전달, 응답 완료 후 DB 저장
- Python: 후속 질문 감지 → 쿼리 확장 → RAG 검색 → 시스템 프롬프트(대화 맥락 규칙 포함) + history 조합 → LLM Chat API 호출

### 2.9 보안 설정

- CSRF 보호: 유지 (X-CSRF-TOKEN 헤더 필요)
- Rate Limit: 기본 100회/분
- 접근 권한: `authenticated()` (로그인 필요)

---

## 3. 공통 인프라

### 3.1 LLM 백엔드

| 구성 요소 | 역할          | 설정                                                                          |
| --------- | ------------- | ----------------------------------------------------------------------------- |
| vLLM      | LLM 추론 서버 | `VLLM_BASE_URL=http://192.168.0.22:9000`, 모델: `qwen2.5-3b`                  |
| Ollama    | 임베딩 모델   | `OLLAMA_BASE_URL=http://host.docker.internal:11434`, 모델: `nomic-embed-text` |
| ChromaDB  | 벡터 저장소   | 포트 8100(외부)/8000(내부), 컬렉션: medical, medical_rules                    |
| MySQL     | 관계형 DB     | 테이블: `medical_rule`, `medical_domain`, `medical_qa`, `medical_content`     |

### 3.2 Spring ↔ Python 통신

```
Spring Boot ──WebClient(llmWebClient)──> Python LLM (FastAPI)
              baseUrl: http://python-llm:8000 (Docker 내부)
              connectTimeout: 5000ms
              readTimeout: 120000ms (LLM 추론 대기)
```

### 3.3 데이터 흐름 요약

```
┌─────────────────────────────────────────────────────────────┐
│                        MySQL                                 │
│  hospital_rule (Admin 관리)  ──인덱싱──>  medical_rule (RAG)  │
│  medical_domain, medical_qa, medical_content (의학 데이터)   │
└─────────────────────────────────────────────────────────────┘
                         │
                    RAG 검색 (LIKE + FULLTEXT)
                         │
┌─────────────────────────────────────────────────────────────┐
│                      ChromaDB                                │
│  medical 컬렉션 (의학 데이터 벡터)                            │
│  medical_rules 컬렉션 (병원 규칙 벡터)                       │
└─────────────────────────────────────────────────────────────┘
                         │
                    벡터 검색 (cosine similarity)
                         │
┌─────────────────────────────────────────────────────────────┐
│                    Python LLM (FastAPI)                       │
│  /infer/medical  — 의학 상담 (RAG + LLM)                     │
│  /infer/rule     — 병원 규칙 Q&A (RAG + LLM)                │
│  /index/rule     — 규칙 인덱싱 API                           │
└─────────────────────────────────────────────────────────────┘
                         │
                    LLM 추론 (vLLM / Ollama)
                         │
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot                                │
│  SymptomAnalysisService — 증상 분석 → 진료과 추천             │
│  ChatService            — 규칙 챗봇 질의 → 응답 + 히스토리   │
│  MedicalService         — 의학 상담 (의사/간호사용)           │
│  RuleIndexService       — Admin 규칙 CUD → 인덱싱 연동       │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 관련 파일 목록

### Spring Boot (Java)

| 파일                                      | 역할                                            |
| ----------------------------------------- | ----------------------------------------------- |
| `llm/controller/SymptomController.java`   | 증상 분석 API                                   |
| `llm/service/SymptomAnalysisService.java` | 증상 분석 서비스 (프롬프트, 파싱, 캐시)         |
| `llm/dto/SymptomRequest.java`             | 증상 분석 요청 DTO                              |
| `llm/dto/SymptomResponse.java`            | 증상 분석 응답 DTO (departmentId, name, reason) |
| `llm/controller/ChatController.java`      | 규칙 챗봇 API                                   |
| `llm/service/ChatService.java`            | 규칙 챗봇 서비스 (LLM 호출, 히스토리 저장)      |
| `llm/controller/MedicalController.java`   | 의학 상담 API (의사/간호사용)                   |
| `llm/service/MedicalService.java`         | 의학 상담 서비스                                |
| `llm/service/RuleIndexService.java`       | 규칙 인덱싱 서비스 (Python API 호출)            |
| `config/WebClientConfig.java`             | LLM WebClient 설정 (baseUrl, timeout)           |
| `config/ClaudeApiConfig.java`             | Claude API RestClient 설정 (미사용 대기)        |

### Python LLM (FastAPI)

| 파일                         | 역할                                                |
| ---------------------------- | --------------------------------------------------- |
| `app.py`                     | FastAPI 메인 — 모든 추론/인덱싱 엔드포인트          |
| `prompts/medical_system.txt` | 의학 상담 시스템 프롬프트                           |
| `prompts/rule_system.txt`    | 규칙 챗봇 시스템 프롬프트                           |
| `medical_context_service.py` | 의학 RAG 컨텍스트 빌드 (MySQL + ChromaDB)           |
| `rule_context_service.py`    | 규칙 RAG 컨텍스트 빌드 (카테고리 감지, 키워드 추출) |
| `embedding_service.py`       | Ollama 임베딩 API 호출                              |
| `vllm_service.py`            | vLLM Chat API 호출                                  |
| `ollama_service.py`          | Ollama Chat API 호출                                |
| `index_rule_data.py`         | 규칙 일괄 인덱싱 스크립트 (JSON → MySQL + ChromaDB) |
| `schemas.py`                 | 요청/응답 Pydantic 스키마                           |

### Frontend (Mustache)

| 파일                                       | 역할                                |
| ------------------------------------------ | ----------------------------------- |
| `reservation/symptom-reservation.mustache` | 증상 분석 예약 페이지 (AJAX)        |
| `reservation/direct-reservation.mustache`  | 직접 선택 예약 페이지               |
| `llm/medical.mustache`                     | 의학 상담 챗봇 UI                   |
| `staff/phone-reservation.mustache` UI      |
| `staff/phone-reservation.mustache`         | 접수 직원 전화 예약 (LLM 추천 포함) |
