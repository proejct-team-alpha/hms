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

## 예약 및 조회 흐름

### 예약 흐름

1. `직접 선택 예약`을 선택하면 예약 입력 화면으로 이동합니다.

![예약 흐름 1 - 예약 방식 선택](docs/images/readme/01-reservation-flow-01-choice.png)

2. 환자 정보, 진료과, 전문의, 날짜와 시간을 입력해 예약을 완료합니다.

![예약 흐름 2 - 예약 완료](docs/images/readme/07-reservation-flow-02-form.png)

3. 예약 완료 후 예약 조회 화면으로 이동해 생성된 예약 정보를 확인합니다.

![예약 흐름 3 - 예약 조회](docs/images/readme/08-reservation-flow-03-complete.png)

### 조회 흐름

1. 예약 조회 결과에서 예약 상세 정보와 변경/취소 액션을 확인할 수 있습니다.

![조회 흐름 1 - 조회 결과](docs/images/readme/09-reservation-flow-04-lookup.png)

2. `예약 변경`을 선택하면 수정 화면으로 이동해 예약 정보를 다시 입력할 수 있습니다.

![조회 흐름 2 - 예약 변경 화면](docs/images/readme/10-reservation-flow-05-modify.png)

---

## STAFF 화면 기능

`STAFF` 계정(`staff01 / password123`)으로 로그인하면 원무과 업무 화면을 확인할 수 있습니다.

### 접수/수납 목록

- 당일 접수 및 수납 목록을 날짜, 상태, 진료과, 의료진 기준으로 조회할 수 있습니다.
- 예약 상태에 따라 접수, 취소, 수납 후속 처리를 이어서 수행하는 중심 화면입니다.

![STAFF 접수/수납 목록](docs/images/staff/02-staff-reception-list.png)

### 전화 예약 등록

- 환자 정보와 예약 정보를 입력해 전화 예약을 등록합니다.
- 이름, 연락처, 진료과, 의료진, 예약 일시 입력 흐름이 한 화면에 구성되어 있습니다.

![STAFF 전화 예약 등록](docs/images/staff/03-staff-phone-reservation.png)

### 방문 접수

- 내원 환자를 현장에서 바로 접수하는 화면입니다.
- 예약 환자 정보를 연동해 접수로 전환하는 흐름도 지원합니다.

![STAFF 방문 접수](docs/images/staff/04-staff-walkin-reception.png)

### 물품 출고

- 원무과 사용 물품을 출고하고 당일 사용 이력을 함께 확인합니다.
- 카테고리별 조회와 사용 취소까지 같은 화면에서 처리할 수 있습니다.

![STAFF 물품 출고](docs/images/staff/05-staff-item-use.png)

### AI 챗봇

- 병원 규정 및 의료 정보 관련 질문을 입력할 수 있는 챗봇 화면입니다.
- 업무 중 빠르게 참고 정보를 확인하는 용도로 사용할 수 있습니다.

![STAFF AI 챗봇](docs/images/staff/06-staff-chatbot.png)

### 내 정보 관리

- 개인 정보와 비밀번호를 수정하는 마이페이지 화면입니다.
- 이름, 연락처, 이메일, 비밀번호 변경 항목을 관리합니다.

![STAFF 내 정보 관리](docs/images/staff/07-staff-mypage.png)

---

## DOCTOR 화면 기능

`DOCTOR` 계정(`doctor01 / password123`)으로 로그인하면 진료 중심 화면을 확인할 수 있습니다.

### 진료 현황

- 오늘 진료 대상 환자와 상태를 기준으로 진료 현황을 확인합니다.
- 환자별 진료 상세 화면으로 이동해 진단과 진료 기록을 이어서 작성할 수 있습니다.

![DOCTOR 진료 현황](docs/images/doctor/02-doctor-treatment-list.png)

### AI 챗봇

- 병원 규정과 의료 정보에 대한 질문을 빠르게 확인할 수 있는 화면입니다.
- 진료 중 참고가 필요한 지침을 조회하는 용도로 사용할 수 있습니다.

![DOCTOR AI 챗봇](docs/images/doctor/03-doctor-chatbot.png)

### 내 정보 관리

- 개인 정보와 비밀번호를 관리하는 마이페이지 화면입니다.
- 이름, 연락처, 이메일, 비밀번호 변경 항목을 수정할 수 있습니다.

![DOCTOR 내 정보 관리](docs/images/doctor/04-doctor-mypage.png)

---

## NURSE 화면 기능

`NURSE` 계정(`nurse01 / password123`)으로 로그인하면 간호 업무 화면을 확인할 수 있습니다.

### 환자 현황

- 날짜와 상태 기준으로 환자 현황을 조회하고 대상 환자를 선택할 수 있습니다.
- 예약 및 접수 흐름 이후 환자 상태를 추적하는 중심 화면입니다.

![NURSE 환자 현황](docs/images/nurse/02-nurse-reception-list.png)

### 처치 관리

- 처치 대상 환자 목록과 처치 상태를 확인하는 화면입니다.
- 환자별 처치 진행 상황을 파악하고 후속 업무를 이어갈 수 있습니다.

![NURSE 처치 관리](docs/images/nurse/03-nurse-treatment-list.png)

### AI 챗봇

- 병원 규정과 의료 정보를 질의할 수 있는 보조 화면입니다.
- 간호 업무 중 필요한 지침을 빠르게 확인하는 데 활용할 수 있습니다.

![NURSE AI 챗봇](docs/images/nurse/04-nurse-chatbot.png)

### 내 정보 관리

- 개인 정보와 비밀번호를 수정하는 마이페이지 화면입니다.
- 이름, 연락처, 이메일, 비밀번호 관련 정보를 관리합니다.

![NURSE 내 정보 관리](docs/images/nurse/05-nurse-mypage.png)

---

## ADMIN 화면 기능

`ADMIN` 계정(`admin01 / password123`)으로 로그인하면 병원 운영 전반을 관리하는 화면을 확인할 수 있습니다.

### 전체 예약 목록

- 환자명과 연락처 기준으로 예약 및 접수 현황을 통합 조회합니다.
- 관리자 관점에서 예약 데이터를 전체적으로 점검하는 기본 화면입니다.

![ADMIN 전체 예약 목록](docs/images/admin/02-admin-reservation-list.png)

### 환자 관리

- 환자 목록을 조회하고 환자 상세 정보 관리로 이어지는 화면입니다.
- 검색과 목록 기반 관리가 가능하도록 구성되어 있습니다.

![ADMIN 환자 관리](docs/images/admin/03-admin-patient-list.png)

### 직원 관리

- 직원 목록과 계정 상태를 확인하고 신규 등록 또는 수정 작업으로 이동할 수 있습니다.
- 이름, 로그인 아이디, 부서, 재직 상태 기준 관리가 가능합니다.

![ADMIN 직원 관리](docs/images/admin/04-admin-staff-list.png)

### 물품 목록

- 병원 내 물품 재고를 카테고리별로 조회하고 관리하는 화면입니다.
- 재고 현황을 운영 관점에서 확인할 수 있습니다.

![ADMIN 물품 목록](docs/images/admin/05-admin-item-list.png)

### 병원 규칙 관리

- 병원 규칙을 제목, 카테고리, 활성 여부 기준으로 조회하고 관리합니다.
- 규정성 문서를 운영자가 직접 관리하는 대표 화면입니다.

![ADMIN 병원 규칙 관리](docs/images/admin/06-admin-rule-list.png)

### AI 챗봇

- 병원 규정 및 의료 정보 관련 질문을 할 수 있는 관리자용 챗봇 화면입니다.
- 운영 중 필요한 정보를 빠르게 탐색하는 용도로 사용할 수 있습니다.

![ADMIN AI 챗봇](docs/images/admin/07-admin-chatbot.png)

### 내 정보 관리

- 개인 정보와 비밀번호를 수정하는 관리자 마이페이지 화면입니다.
- 기본 프로필 정보와 계정 정보를 함께 관리할 수 있습니다.

![ADMIN 내 정보 관리](docs/images/admin/08-admin-mypage.png)

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
