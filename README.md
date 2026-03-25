# 🏥 병원 예약 & 내부 업무 시스템 (HMS)

> Spring Boot 기반 병원 예약·접수·진료·관리 통합 시스템
> AI 증상 추천 예약 + 병원 규칙 Q&A 챗봇 포함

---

## 기술 스택

| 분류        | 기술                                                        |
| ----------- | ----------------------------------------------------------- |
| Language    | Java 21, Python 3.14                                        |
| Framework   | Spring Boot 4.x, FastAPI 0.115                              |
| Security    | Spring Security (세션 기반, 5 ROLE)                         |
| View        | Mustache (SSR)                                              |
| CSS         | Tailwind CSS 4.x (CLI 빌드)                                 |
| JS Vendor   | Lucide Icons, Chart.js, Flatpickr                           |
| ORM         | Spring Data JPA                                             |
| DB          | H2 (개발) / MySQL 8.x (운영), Redis (캐시/세션)             |
| AI (Java)   | Claude API (Anthropic) — 병원 규칙 Q&A 챗봇                 |
| AI (Python) | Qwen 2.5 (vLLM/Ollama) + ChromaDB RAG — 의료 상담/증상 분석 |
| Build       | Gradle (백엔드) + npm (프론트엔드)                          |
| Test        | JUnit 5, Mockito 5.20, Spring MockMvc, REST Docs            |

---

## 주요 기능

**비회원 (환자)**

- AI 증상 입력 → 진료과·의사 자동 추천 예약
- 진료과·의사 직접 선택 예약
- 예약번호 발급

**내부 직원**

- `STAFF` — 전화 예약 등록 / 방문 접수 / 접수 처리 / 물품 사용
- `DOCTOR` — 오늘 진료 목록 / 진료 기록 입력 / 병원 규칙 Q&A 챗봇
- `NURSE` — 예약 현황 조회 / 환자 정보 수정 / 병원 규칙 Q&A 챗봇
- `ITEM_MANAGER` — 물품 입고·출고·재고 관리 / 사용 이력 / 대시보드
- `ADMIN` — 예약·환자·직원·진료과·물품·병원 규칙 전체 관리 / 대시보드

---

## 로컬 실행

### 사전 준비

- Java 21
- Node.js 18+ (프론트엔드 빌드)
- Claude API Key ([Anthropic Console](https://console.anthropic.com) 발급) — LLM 기능 사용 시

### 실행 방법

```bash
# 1. 저장소 클론
git clone https://github.com/proejct-team-alpha/hospital-reservation-system.git
cd hospital-reservation-system

# 2. 환경 변수 파일 생성 (git에 포함되지 않음)
cp .env.example .env
# .env 에 CLAUDE_API_KEY=sk-ant-... 입력

# 3. 프론트엔드 빌드
npm install          # 의존성 설치 (최초 1회)
npm run build        # Tailwind CSS 빌드 + Vendor JS 복사

# 4. 서버 실행 (Windows PowerShell)
.\run-dev.ps1

# 또는 Gradle 직접 실행
.\gradlew bootRun

# 5. 접속
# http://localhost:9090
```

### 프론트엔드 개발 (CSS 실시간 반영)

```bash
npm run dev          # Vendor JS 복사 + Tailwind CSS watch 모드
```

별도 터미널에서 `gradlew bootRun`을 함께 실행하면 CSS 변경이 즉시 반영됩니다.

### Python LLM 서버 (의료 상담·증상 분석용, 선택)

```bash
cd python-llm
python -m venv .venv
.venv\Scripts\activate       # Windows
pip install -r requirements.txt

# .env 에 설정 (기본값으로도 동작)
# LLM_BACKEND=vllm           # vllm | ollama | huggingface
# LLM_MODEL=Qwen/Qwen2.5-3B-Instruct

uvicorn app:app --host 0.0.0.0 --port 8000
```

Spring Boot에서 `llm.service.url` 프로퍼티로 연결합니다 (기본: `http://192.168.0.22:8000`).

### npm 스크립트 요약

| 명령어              | 설명                                                   |
| ------------------- | ------------------------------------------------------ |
| `npm install`       | 의존성 설치 (최초 1회 또는 package.json 변경 시)       |
| `npm run build`     | CSS 빌드 + Vendor JS 복사 (배포/CI용)                  |
| `npm run build:css` | Tailwind CSS만 빌드 (`input.css` → `tailwind.min.css`) |
| `npm run build:js`  | Vendor JS만 복사 (lucide, chart.js)                    |
| `npm run dev`       | CSS watch 모드 (개발용, 변경 감지 자동 빌드)           |

### 개발 환경

- **프로필**: `dev` (기본)
- **DB**: H2 in-memory (별도 설치 불필요)
- **H2 콘솔**: <http://localhost:9090/h2-console>
- **테스트 데이터**: `src/main/resources/sql_test.sql` (H2용 테스트 데이터 & 로그인 정보)
- **테스트 계정**: `admin01` / `staff01` / `doctor01` / `nurse01` / `item01` (비밀번호: `password123`)

| 아이디   | 비밀번호    | 역할         |
| -------- | ----------- | ------------ |
| admin01  | password123 | ADMIN        |
| staff01  | password123 | STAFF        |
| doctor01 | password123 | DOCTOR       |
| nurse01  | password123 | NURSE        |
| item01   | password123 | ITEM_MANAGER |

> ⚠️ `.env` 는 `.gitignore` 등록됨 — 절대 커밋 금지

---

## 프로젝트 구조

```
hospital-reservation-system/
├── package.json                 # 프론트엔드 빌드 설정 (npm)
├── scripts/copy-vendor.js       # Vendor JS 복사 스크립트
├── build.gradle                 # 백엔드 빌드 설정 (Gradle)
│
├── src/main/java/com/smartclinic/hms/
│   ├── config/                  # Security, MVC, WebClient, Cache, Async, RateLimit
│   ├── common/                  # 공통 인터셉터, 예외 처리, 유틸
│   ├── domain/                  # JPA Entity (15개) + Enum (5개)
│   ├── auth/                    # 인증 (로그인, UserDetails)
│   ├── home/                    # 메인 페이지, 역할 선택
│   ├── reservation/             # 비회원 예약 (직접/증상 기반)
│   ├── staff/                   # 접수 직원 (접수, 전화예약, 방문접수, 대시보드)
│   ├── doctor/                  # 의사 (진료 목록, 진료 기록, 마이페이지)
│   ├── nurse/                   # 간호사 (접수 현황, 환자 관리, 마이페이지)
│   ├── item/                    # 물품관리자 (재고, 입출고, 사용 이력, 대시보드)
│   ├── admin/                   # 관리자 (대시보드, 예약/환자/직원/진료과/물품/규칙 CRUD)
│   └── llm/                     # LLM 연동 (챗봇, 증상 분석, 의료 상담, 예약 추천)
│
├── src/test/java/               # 테스트 (42 클래스, 249 케이스)
│   └── com/smartclinic/hms/     # Controller·Service·Repository·Domain 테스트
│
├── src/main/resources/
│   ├── templates/               # Mustache 템플릿 (74개)
│   │   ├── common/              # 헤더·사이드바·푸터 파셜 (역할별 분리)
│   │   ├── admin/               # 관리자 화면
│   │   ├── staff/               # 접수 직원 화면
│   │   ├── doctor/              # 의사 화면
│   │   ├── nurse/               # 간호사 화면
│   │   ├── item-manager/        # 물품관리자 화면
│   │   ├── reservation/         # 예약 화면
│   │   ├── llm/                 # LLM 챗봇·의료 상담 화면
│   │   ├── auth/                # 로그인
│   │   ├── home/                # 메인 페이지
│   │   └── error/               # 403, 404, 500
│   ├── static/
│   │   ├── css/input.css        # Tailwind CSS 소스
│   │   ├── js/app.js            # 공통 JS (lucide init)
│   │   └── js/pages/            # 페이지별 JS (차트, 폼 등)
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   └── sql_test.sql             # H2 테스트 데이터 & 로그인 정보
│
├── python-llm/                  # Python LLM 추론 서버 (RAG 파이프라인)
│   ├── app.py                   # FastAPI 앱 진입점
│   ├── config.py                # 환경 설정 (Pydantic Settings)
│   ├── llm_service.py           # LLM 호출 (vLLM/Ollama/HuggingFace)
│   ├── embedding_service.py     # 임베딩 생성
│   ├── vector_store.py          # ChromaDB 벡터 저장소
│   ├── reranker.py              # 검색 결과 리랭킹
│   ├── chunker.py               # 문서 청킹
│   ├── query_expander.py        # 쿼리 확장
│   ├── typo_corrector.py        # 오타 교정
│   ├── response_cleaner.py      # 응답 정제
│   ├── prompt_loader.py         # 프롬프트 템플릿
│   ├── circuit_breaker.py       # 서킷 브레이커 (장애 대응)
│   ├── medical_context_service.py  # 의료 데이터 RAG 컨텍스트
│   ├── rule_context_service.py  # 병원 규칙 RAG 컨텍스트
│   └── requirements.txt         # Python 의존성
│
└── doc/                         # 프로젝트 문서
    ├── PRD.md                   # 요구사항 정의서
    ├── API.md                   # API 명세서
    ├── SKILL_*.md               # 개발자별 작업 명세
    ├── LLM_FEATURE_ARCHITECTURE.md  # LLM 기능 아키텍처
    └── 테스트_보고서.md          # 테스트 결과 보고서
```

---

## 팀 구성 및 담당 영역

| 역할               | 담당 영역                                                          |
| ------------------ | ------------------------------------------------------------------ |
| Lead 개발자        | 아키텍처·Security·Entity·LLM 연동·Python LLM 서버·배포             |
| 개발자 A           | 비회원 예약 흐름 (`/reservation/**`)                               |
| 개발자 B           | 내부 직원 화면 (`/staff/**` · `/doctor/**` · `/nurse/**`) + LLM UI |
| 개발자 C           | 관리자 화면 (`/admin/**` · `/item/**`)                             |

---

## 테스트

```bash
./gradlew test
```

| 항목          | 값    |
| ------------- | ----- |
| 테스트 클래스 | 42개  |
| 테스트 케이스 | 249개 |
| 성공률        | 100%  |

테스트 유형: Controller(MockMvc) 130건, Service(Unit) 100건, Repository(DataJpa) 8건, Domain/DTO/Interceptor/Scheduler 11건

상세 결과: [doc/테스트\_보고서.md](doc/테스트_보고서.md)

---

## 브랜치 전략

```
main       ← 배포 가능 안정 버전 (경력자 리뷰 필수)
develop    ← 통합 브랜치 (매주 금요일 머지)
feature/*  ← 기능별 개발 브랜치
```

---

## 관련 문서

| 문서                                                                            | 비고                                             |
| ------------------------------------------------------------------------------- | ------------------------------------------------ |
| [proejct-team-alpha/documents](https://github.com/proejct-team-alpha/documents) | 프로젝트 계획서, API 명세서, 화면 정의서, ERD 등 |
| [doc/PRD.md](doc/PRD.md)                                                        | 요구사항 정의서 (PRD)                            |
| [doc/SKILL_LEAD.md](doc/SKILL_LEAD.md)                                          | 책임개발자 작업 명세                             |
| [doc/SKILL_DEV_A.md](doc/SKILL_DEV_A.md)                                        | 개발자 A 작업 명세                               |
| [doc/SKILL_DEV_B.md](doc/SKILL_DEV_B.md)                                        | 개발자 B 작업 명세                               |
| [doc/SKILL_DEV_C.md](doc/SKILL_DEV_C.md)                                        | 개발자 C 작업 명세                               |
| [doc/PRE_WORK.md](doc/PRE_WORK.md)                                              | 선행 작업 체크리스트                             |
| [doc/API.md](doc/API.md)                                                        | API 명세서                                       |
| [doc/LLM_FEATURE_ARCHITECTURE.md](doc/LLM_FEATURE_ARCHITECTURE.md)              | LLM 기능 아키텍처                                |
| [doc/테스트\_보고서.md](doc/테스트_보고서.md)                                   | 테스트 결과 보고서 (249 케이스)                  |
