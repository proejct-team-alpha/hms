# LLM + RAG 기술 발표 대본

## 병원 예약 시스템 (Hospital Management System) - AI 기능 기술 발표

---

## 목차

1. [전체 아키텍처 개요](#1-전체-아키텍처-개요)
2. [사용된 LLM 모델](#2-사용된-llm-모델)
3. [RAG 파이프라인 구성](#3-rag-파이프라인-구성)
4. [설치 및 환경 설정](#4-설치-및-환경-설정)
5. [핵심 기능별 동작 흐름](#5-핵심-기능별-동작-흐름)
6. [프론트엔드 연동](#6-프론트엔드-연동)
7. [운영 안정성 (Resilience)](#7-운영-안정성-resilience)
8. [정리 및 요약](#8-정리-및-요약)

---

## 1. 전체 아키텍처 개요

> 안녕하세요. 저희 병원 예약 시스템에 적용된 LLM과 RAG 기술에 대해 발표하겠습니다.

저희 시스템은 **Spring Boot(Java) 백엔드**와 **FastAPI(Python) LLM 서버**가 분리된 **마이크로서비스 구조**로 되어 있습니다.

### 전체 구성도

```
[사용자 브라우저]
      |
   [Nginx] (리버스 프록시, SSL)
      |
[Spring Boot - 포트 8080]  ←→  [Redis - 세션 캐시]
      |                          |
      |                     [MySQL 8.0 - 메인 DB]
      |
      ↓  (WebClient 비동기 호출)
[Python FastAPI LLM 서버 - 포트 8000]
      |              |
[vLLM / Ollama]   [ChromaDB - 벡터 DB]
 (LLM 추론)       (RAG 문서 저장소)
```

### Docker Compose 서비스 구성

| 서비스 | 이미지 | 포트 | 역할 |
|--------|--------|------|------|
| **mysql** | mysql:8.0 | 3306 | 메인 데이터베이스 |
| **redis** | redis:7-alpine | 6379 | 세션 캐시 |
| **chromadb** | chromadb/chroma:1.5.4 | 8100 | 벡터 데이터베이스 |
| **python-llm** | 자체 빌드 (FastAPI) | 8000 | LLM 추론 서버 |
| **spring-app** | 자체 빌드 (Spring Boot) | 8080 | 메인 애플리케이션 |
| **nginx** | nginx:alpine | 80, 443 | 리버스 프록시 |

> 이처럼 총 6개의 컨테이너가 Docker Compose로 오케스트레이션되며, 각 서비스는 독립적으로 스케일링이 가능합니다.

---

## 2. 사용된 LLM 모델

> 다음으로, 저희가 사용한 LLM 모델에 대해 설명드리겠습니다.

### 2.1 메인 모델: Qwen 2.5 3B Instruct

- **모델명**: `Qwen/Qwen2.5-3B-Instruct`
- **개발사**: Alibaba Cloud (통의치엔원)
- **파라미터 수**: 30억 (3B)
- **특징**: 한국어를 포함한 다국어 지원, 의료 상담에 적합한 지시 추종(Instruction Following) 능력
- **선택 이유**: 3B 규모로 로컬 서버에서 실시간 추론이 가능하면서도 충분한 한국어 응답 품질 확보

### 2.2 임베딩 모델: nomic-embed-text

- **모델명**: `nomic-embed-text`
- **용도**: 문서를 벡터로 변환하여 ChromaDB에 저장
- **실행 방식**: Ollama를 통해 로컬에서 임베딩 생성
- **특징**: 텍스트를 고차원 벡터 공간에 매핑하여 의미 기반 유사도 검색 가능

### 2.3 지원하는 3가지 LLM 백엔드

저희 시스템은 환경에 따라 **3가지 LLM 백엔드**를 선택적으로 사용할 수 있도록 설계했습니다.

| 백엔드 | 설정값 | 특징 | 사용 시나리오 |
|--------|--------|------|--------------|
| **vLLM** | `LLM_BACKEND=vllm` | OpenAI 호환 API, 고성능 배치 추론 | **운영 환경 (기본값)** |
| **Ollama** | `LLM_BACKEND=ollama` | 간편 설치, 임베딩 겸용 | 개발/테스트 환경 |
| **HuggingFace** | `LLM_BACKEND=huggingface` | transformers 직접 로딩 | 로컬 개발, GPU 직접 사용 |

> 현재 운영 환경에서는 **vLLM**을 기본 백엔드로 사용하고 있으며, `http://192.168.0.22:9000`에서 vLLM 서버가 구동됩니다. Ollama는 임베딩 생성과 개발 환경에서 활용됩니다.

---

## 3. RAG 파이프라인 구성

> 이제 저희 시스템의 핵심인 RAG 파이프라인에 대해 설명드리겠습니다.

### 3.1 RAG란?

**RAG(Retrieval-Augmented Generation)**은 LLM이 응답을 생성하기 전에, 관련 문서를 검색하여 컨텍스트로 제공하는 기법입니다. 이를 통해:

- LLM의 **환각(Hallucination)을 줄이고**
- **최신 정보**를 반영할 수 있으며
- **도메인 특화 지식**을 활용할 수 있습니다

### 3.2 벡터 데이터베이스: ChromaDB

- **버전**: chromadb 0.5.x ~ 1.0.0 미만
- **Docker 이미지**: `chromadb/chroma:1.5.4`
- **포트**: 8100 (외부) -> 8000 (내부)
- **거리 측정 방식**: 코사인 유사도 (Cosine Similarity)

#### HNSW 인덱스 설정

```python
HNSW_CONFIG = {
    "hnsw:space": "cosine",         # 코사인 유사도 사용
    "hnsw:M": 16,                   # 노드당 최대 연결 수
    "hnsw:construction_ef": 200,    # 인덱스 구축 시 탐색 범위
    "hnsw:search_ef": 50,           # 검색 시 탐색 범위
    "hnsw:num_threads": 4           # 병렬 처리 스레드 수
}
```

> HNSW(Hierarchical Navigable Small World)는 근사 최근접 이웃 검색 알고리즘으로, 대규모 벡터 데이터에서도 빠른 유사도 검색이 가능합니다.

#### 2개의 벡터 컬렉션

| 컬렉션 | 환경변수 | 용도 |
|---------|----------|------|
| `medical_docs` | `CHROMA_COLLECTION` | 의료 지식 문서 (진료 상담용) |
| `medical_rules` | `CHROMA_RULE_COLLECTION` | 병원 내규/규정 (직원 챗봇용) |

### 3.3 하이브리드 검색 전략

저희는 벡터 검색만 사용하는 것이 아니라, **하이브리드 검색**을 적용했습니다.

```
사용자 질문
    |
    ├── [벡터 검색] ChromaDB 코사인 유사도 (Top-K=3)
    |       └── 의미적으로 유사한 문서 검색
    |
    └── [키워드 검색] MySQL FULLTEXT / LIKE 검색
            └── 정확한 키워드 매칭
    |
    ↓ (결과 병합)
  컨텍스트 구성 → LLM 프롬프트에 주입
```

- **벡터 검색**: 의미적 유사도 기반, `VECTOR_DISTANCE_THRESHOLD=0.5` 이하만 채택
- **키워드 검색**: MySQL의 `FULLTEXT` 인덱스(ngram) 및 `LIKE` 패턴 매칭
- **결과 병합**: 두 검색 결과를 합쳐 중복 제거 후 LLM에 전달

### 3.4 문서 인덱싱 파이프라인

```
관리자가 병원 규정 등록/수정
    ↓
Spring Boot → hospital_rule 테이블 저장
    ↓ (비동기 호출)
Python FastAPI POST /index/rule
    ↓
Ollama로 텍스트 임베딩 생성
    ↓
ChromaDB medical_rules 컬렉션에 벡터 저장
    ↓
이후 사용자 질문 시 RAG 검색에 활용
```

> 관리자가 규정을 등록하면 자동으로 벡터 인덱싱이 이루어지므로, 별도의 수동 인덱싱 작업이 필요 없습니다.

---

## 4. 설치 및 환경 설정

> 실제 설치 및 설정 과정을 말씀드리겠습니다.

### 4.1 Python LLM 서버 의존성

**파일 위치**: `python-llm/requirements.txt`

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| **FastAPI** | ~0.115.0 | REST API 프레임워크 |
| **uvicorn** | ~0.34.0 | ASGI 서버 |
| **transformers** | ~4.46.0 | HuggingFace 모델 로딩/추론 |
| **torch** | >=2.1.0, <3.0.0 | PyTorch 딥러닝 프레임워크 |
| **accelerate** | ~1.1.0 | 분산 추론 가속 |
| **chromadb** | >=0.5.0, <1.0.0 | 벡터 DB 클라이언트 |
| **pydantic** | ~2.10.0 | 데이터 검증 |
| **pydantic-settings** | ~2.6.0 | 환경변수 기반 설정 관리 |
| **pymysql** | ~1.1.0 | MySQL 동기 드라이버 |
| **aiomysql** | ~0.2.0 | MySQL 비동기 드라이버 |
| **httpx** | ~0.27.0 | 비동기 HTTP 클라이언트 |
| **slowapi** | ~0.1.9 | API 요청 제한(Rate Limiting) |

### 4.2 Spring Boot 의존성 (build.gradle)

- **Spring Boot** 4.0.3 / **Java 21**
- `spring-boot-starter-webflux` - 비동기 리액티브 WebClient (Python LLM 호출용)
- `spring-boot-starter-web` - REST API
- `spring-boot-starter-data-jpa` - ORM
- `spring-boot-starter-security` - 인증/인가
- `spring-boot-starter-data-redis` - 세션 캐시
- `mysql-connector-j` - MySQL 드라이버

### 4.3 핵심 환경변수 설정

#### Python LLM 서버 (.env)

```bash
# LLM 백엔드 선택
LLM_BACKEND=vllm                          # huggingface | ollama | vllm

# vLLM 설정 (운영 환경)
VLLM_BASE_URL=http://192.168.0.22:9000    # vLLM 서버 주소
VLLM_MODEL=qwen2.5-3b                     # 모델명

# Ollama 설정 (개발 환경 / 임베딩)
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=qwen2.5:3b                   # LLM 모델
OLLAMA_EMBED_MODEL=nomic-embed-text        # 임베딩 모델

# HuggingFace 설정 (직접 로딩)
LLM_MODEL=Qwen/Qwen2.5-3B-Instruct

# ChromaDB 설정
CHROMA_HOST=chromadb
CHROMA_PORT=8000
CHROMA_COLLECTION=medical_docs             # 의료 문서 컬렉션
CHROMA_RULE_COLLECTION=medical_rules       # 병원 규정 컬렉션

# 검색 설정
VECTOR_SEARCH_TOP_K=3                      # 상위 K개 결과 반환
USE_VECTOR_SEARCH=true                     # 벡터 검색 활성화
USE_QUERY_EXPANSION=false                  # 쿼리 확장 (실험적)
USE_RERANKING=false                        # 리랭킹 (실험적)

# MySQL 연결
MYSQL_HOST=mysql
MYSQL_USER=root
MYSQL_PASSWORD=rootpassword
MYSQL_DB=hms_db

# 추론 설정
LLM_INFER_TIMEOUT_SEC=60                   # 추론 타임아웃(초)
LLM_INPUT_MAX_LENGTH=2048                  # 최대 입력 길이
LLM_FALLBACK_MOCK=false                    # Mock 응답 사용 여부
```

#### Spring Boot (application.yml / 환경변수)

```bash
LLM_SERVICE_URL=http://python-llm:8000     # Python LLM 서버 주소
CLAUDE_API_KEY=                             # Anthropic API 키 (미래 확장용, 현재 미사용)
```

### 4.4 Docker Compose로 전체 실행

```bash
# 전체 서비스 빌드 및 실행
docker-compose up --build -d

# 서비스 상태 확인
docker-compose ps

# LLM 서버 헬스체크
curl http://localhost:8000/health
```

> Docker Compose의 `depends_on` + `healthcheck`를 통해 MySQL과 ChromaDB가 정상 기동된 후에 Python LLM 서버가 시작되도록 의존성 순서를 보장합니다.

---

## 5. 핵심 기능별 동작 흐름

> 저희 시스템에서 LLM과 RAG가 활용되는 핵심 기능 3가지를 설명드리겠습니다.

### 5.1 기능 1: 증상 기반 진료과 추천 (비회원 예약)

**대상**: 비회원 사용자 (인증 불요)
**엔드포인트**: `POST /llm/symptom/analyze`
**Rate Limit**: IP당 분당 20회

```
[비회원] "머리가 아프고 어지러워요"
    ↓
[Spring Boot] SymptomController
    ↓
[SymptomAnalysisService] → Python LLM POST /infer/medical
    ↓
[Python LLM]
  1. MySQL FULLTEXT 검색 (medical_qa, medical_domain 테이블)
  2. ChromaDB 벡터 검색 (medical_docs 컬렉션)
  3. 검색된 의료 지식을 컨텍스트로 구성
  4. 시스템 프롬프트 + 컨텍스트 + 사용자 질문 → Qwen 2.5 3B 추론
    ↓
[응답] "신경과 진료를 추천드립니다. 두통과 어지러움은..."
    ↓
[Spring Boot] 응답에서 진료과 추출 (정규식 패턴 매칭)
    ↓
[결과] { departmentId: 5, departmentName: "신경과", reason: "..." }
    ↓
[화면] 추천 진료과 → 해당 과 의사 목록 → 예약 진행
```

### 5.2 기능 2: 병원 내규 Q&A 챗봇 (직원용)

**대상**: 인증된 직원 (의사, 간호사, 수간호사, 관리자)
**엔드포인트**: `POST /llm/chatbot/query` (일반) / `POST /llm/chatbot/query/stream` (스트리밍)

```
[직원] "감염 환자 격리 절차가 어떻게 되나요?"
    ↓
[Spring Boot] ChatController
    ↓
[ChatService] → Python LLM POST /infer/rule
    ↓
[Python LLM - rule_context_service.py]
  1. 카테고리 분류: "위생/감염" 카테고리로 판별
  2. 한국어 조사 제거, 노이즈 단어 필터링
  3. ChromaDB 벡터 검색 (medical_rules 컬렉션)
     - 코사인 유사도 0.5 이하만 채택
  4. MySQL LIKE 키워드 검색 (hospital_rule 테이블)
  5. 하이브리드 결과 병합 → 컨텍스트 구성
  6. 시스템 프롬프트 + 컨텍스트 + 대화 이력 → LLM 추론
    ↓
[응답] "감염 환자 격리 절차는 다음과 같습니다..."
    ↓
[Spring Boot] chatbot_history 테이블에 대화 기록 저장
    ↓
[화면] 챗봇 UI에 응답 표시
```

#### 카테고리 기반 필터링

규정 검색 시 질문을 다음 9개 카테고리로 자동 분류합니다:

| 카테고리 | 관련 키워드 예시 |
|----------|-----------------|
| 위생/감염 | 감염, 격리, 소독, 위생 |
| 당직/근무 | 당직, 근무, 교대, 야간 |
| 물품/비품 | 물품, 비품, 재고, 발주 |
| 응급 | 응급, 긴급, CPR, 심정지 |
| 수술 | 수술, 마취, 수술실 |
| 입원/퇴원 | 입원, 퇴원, 병동, 병실 |
| 안전 | 안전, 화재, 대피, 사고 |
| 투약/처방 | 투약, 처방, 약물, 주사 |
| 검사/진단 | 검사, 진단, 혈액, CT |

### 5.3 기능 3: 의료 상담 (의사/간호사용)

**대상**: 인증된 의료진 (의사, 간호사)
**엔드포인트**: `POST /llm/medical/query` / `POST /llm/medical/query/consult`

```
[의료진] "고혈압 환자의 약물 치료 가이드라인은?"
    ↓
[MedicalController] → [MedicalService]
    ↓
[Python LLM POST /infer/medical]
  1. 의료 컨텍스트 검색 (MySQL + ChromaDB)
  2. LLM 추론 (의료 시스템 프롬프트 적용)
    ↓
[응답] 의료 상담 결과 + 추천 진료과 + 해당 과 의사 목록
    ↓
[medical_history 테이블에 기록 저장]
```

### 5.4 멀티턴 대화 지원

세 기능 모두 **멀티턴(Multi-turn) 대화**를 지원합니다:

- 브라우저에서 `history[]` 배열로 대화 이력 관리
- 최근 **6개 메시지**를 Python LLM에 전달
- 후속 질문 감지 (대명사, 짧은 질문 패턴)
- 대화 이력 기반 쿼리 확장

```javascript
// 프론트엔드 대화 이력 관리 예시
const history = [
  { role: "user", content: "감염 환자 격리 절차가 어떻게 되나요?" },
  { role: "assistant", content: "감염 환자 격리 절차는..." },
  { role: "user", content: "그러면 보호구는 어떤 걸 착용해야 하나요?" }  // 후속 질문
];
```

---

## 6. 프론트엔드 연동

> 프론트엔드에서 LLM 기능이 어떻게 표현되는지 설명드리겠습니다.

### 6.1 페이지 구성 (Mustache 템플릿)

| 템플릿 | 용도 | 접근 권한 |
|--------|------|----------|
| `reservation/symptom-reservation.mustache` | 증상 분석 → 진료과 추천 → 예약 | 비회원 |
| `llm/chatbot.mustache` | 병원 내규 Q&A 챗봇 | 전 직원 |
| `doctor/chatbot.mustache` | 의사 전용 내규 챗봇 | 의사 |
| `nurse/chatbot.mustache` | 간호사 전용 내규 챗봇 | 간호사 |
| `staff/chatbot.mustache` | 접수원 전용 내규 챗봇 | 접수원 |
| `admin/chatbot.mustache` | 관리자 규정 관리 + 챗봇 | 관리자 |
| `llm/medical.mustache` | 의료 상담 | 의사/간호사 |

### 6.2 스트리밍 응답 (Server-Sent Events)

실시간 타이핑 효과를 위해 **SSE(Server-Sent Events)** 방식의 스트리밍을 지원합니다:

```
[브라우저] EventSource → [Spring Boot] /llm/chatbot/query/stream
    → [Python LLM] /infer/rule/stream
    → 토큰 단위 스트리밍 응답
    → 화면에 실시간 글자 표시
```

---

## 7. 운영 안정성 (Resilience)

> 운영 환경에서의 안정성 확보 방안을 설명드리겠습니다.

### 7.1 서킷 브레이커 (Circuit Breaker)

vLLM과 Ollama 서비스에 서킷 브레이커 패턴을 적용했습니다:

```
정상 상태 (CLOSED)
    ↓ 연속 5회 실패
차단 상태 (OPEN) → 30초 대기 → 반개방 (HALF-OPEN) → 성공 시 정상 복귀
```

- **실패 임계값**: 5회 연속 실패
- **복구 대기**: 30초 후 재시도
- **효과**: LLM 서버 장애 시 불필요한 요청 차단, 빠른 폴백 응답 반환

### 7.2 Rate Limiting (요청 제한)

| 구간 | 제한 | 도구 |
|------|------|------|
| Python LLM 서버 | 엔드포인트별 제한 | slowapi |
| 증상 분석 API | IP당 분당 20회 | Spring Boot 커스텀 |

### 7.3 타임아웃 설정

| 구간 | 연결 타임아웃 | 읽기 타임아웃 |
|------|-------------|-------------|
| Spring → Python | 5,000ms | 30,000ms (스트리밍: 120,000ms) |
| Python LLM 추론 | - | 60,000ms |

### 7.4 폴백 및 에러 처리

- **LLM 서버 불가 시**: `LlmServiceUnavailableException` → 사용자에게 안내 메시지
- **추론 타임아웃 시**: `LlmTimeoutException` → 재시도 안내
- **진료과 추출 실패 시**: 기본값 "내과"로 폴백
- **Mock 모드**: `LLM_FALLBACK_MOCK=true` 설정 시 torch 없이도 테스트 가능

---

## 8. 정리 및 요약

> 마지막으로 전체 내용을 정리하겠습니다.

### 기술 스택 한눈에 보기

| 구분 | 기술 |
|------|------|
| **LLM 모델** | Qwen 2.5 3B Instruct |
| **임베딩 모델** | nomic-embed-text |
| **LLM 서빙** | vLLM (운영) / Ollama (개발) / HuggingFace (로컬) |
| **벡터 DB** | ChromaDB 1.5.4 (HNSW, 코사인 유사도) |
| **RAG 검색** | 하이브리드 (벡터 + 키워드) |
| **LLM API 서버** | FastAPI + Uvicorn (Python) |
| **백엔드 연동** | Spring Boot WebFlux WebClient (비동기) |
| **스트리밍** | SSE (Server-Sent Events) |
| **안정성** | 서킷 브레이커, Rate Limiting, 타임아웃, 폴백 |
| **인프라** | Docker Compose (6개 서비스) |

### AI 기능 사용 범위

| 기능 | 대상 | RAG 소스 | 인증 |
|------|------|----------|------|
| 증상 → 진료과 추천 | 비회원 | 의료 지식 DB | 불요 |
| 병원 내규 Q&A | 전 직원 | 병원 규정 DB | 필요 |
| 의료 상담 | 의사/간호사 | 의료 지식 DB | 필요 |
| 규정 자동 인덱싱 | 관리자 | - | 필요 |

### 핵심 설계 포인트

1. **마이크로서비스 분리**: Java 백엔드와 Python LLM 서버를 분리하여 독립적 배포/스케일링 가능
2. **하이브리드 RAG**: 벡터 검색과 키워드 검색을 병합하여 검색 정확도 향상
3. **다중 백엔드 지원**: vLLM, Ollama, HuggingFace 중 환경에 맞는 백엔드 선택
4. **한국어 특화 전처리**: 한국어 조사 제거, 의료 용어 오타 교정, 노이즈 필터링
5. **운영 안정성**: 서킷 브레이커, 타임아웃, 폴백으로 장애 전파 방지

> 이상으로 저희 병원 예약 시스템의 LLM 및 RAG 기술에 대한 발표를 마치겠습니다. 감사합니다.

---

### 참고 문서

- `doc/LLM_FEATURE_ARCHITECTURE.md` - LLM 시스템 상세 설계 문서
- `AI-CONTEXT.md` - 프로젝트 전체 기술 스택 및 컨텍스트
- `python-llm/README.md` - Python LLM 서버 설정 가이드
- `docker-compose.yml` - 전체 인프라 구성
