# 🏥 병원 예약 & 내부 업무 시스템 (HMS)

> Spring Boot 기반 병원 예약·접수·진료·관리 통합 시스템
> AI 증상 추천 예약 + 병원 규칙 Q&A 챗봇 포함

---

## 기술 스택

| 분류      | 기술                                |
| --------- | ----------------------------------- |
| Language  | Java 21                             |
| Framework | Spring Boot 4.x                     |
| Security  | Spring Security (세션 기반, 4 ROLE) |
| View      | Mustache (SSR)                      |
| ORM       | Spring Data JPA                     |
| DB        | H2 (개발) / MySQL 8.x (운영)        |
| AI        | Claude API (Anthropic)              |
| Build     | Gradle                              |

---

## 주요 기능

**비회원 (환자)**

- AI 증상 입력 → 진료과·의사 자동 추천 예약
- 진료과·의사 직접 선택 예약
- 예약번호 발급

**내부 직원**

- `STAFF` — 전화 예약 등록 / 방문 접수 / 접수 처리
- `DOCTOR` — 오늘 진료 목록 / 진료 기록 입력 / 병원 규칙 Q&A 챗봇
- `NURSE` — 예약 현황 조회 / 환자 정보 수정 / 병원 규칙 Q&A 챗봇
- `ADMIN` — 예약·직원·진료과·물품·병원 규칙 전체 관리

---

## 로컬 실행

### 사전 준비

- Java 21
- Claude API Key ([Anthropic Console](https://console.anthropic.com) 발급) — LLM 기능 사용 시

### 실행 방법

```bash
# 1. 저장소 클론
git clone https://github.com/[org]/hms.git
cd hms

# 2. 환경 변수 파일 생성 (git에 포함되지 않음)
cp .env.example .env
# .env 에 CLAUDE_API_KEY=sk-ant-... 입력

# 3. 실행 (Windows PowerShell)
.\run-dev.ps1

# 또는 Gradle 직접 실행
.\gradlew bootRun

# 4. 접속
# http://localhost:8080
```

### 개발 환경

- **프로필**: `dev` (기본)
- **DB**: H2 in-memory (별도 설치 불필요)
- **H2 콘솔**: <http://localhost:8080/h2-console>
- **테스트 계정**: `admin01` / `staff01` / `doctor01` / `nurse01` (비밀번호: `password123`)

> ⚠️ `.env` 는 `.gitignore` 등록됨 — 절대 커밋 금지

---

## 프로젝트 구조

```
src/main/java/com/smartclinic/hms/
├── config/              # Security, MVC, Claude API 설정
├── common/              # 공통 인터셉터, 예외 처리, 유틸, 서비스
├── domain/              # JPA Entity (예정)
├── admin/               # 관리자 화면 (dashboard, staff, patient, rule, item, department, reservation, reception)
├── staff/               # 접수 직원 화면 (dashboard, reception, reservation, walkin)
├── doctor/              # 의사 화면 (treatment)
├── nurse/               # 간호사 화면 (schedule, patient)
├── reservation/          # 외부 예약 흐름
├── llm/                 # Claude API 연동
└── _sample/             # 샘플 코드 (참고용)

src/main/resources/
├── templates/
│   ├── common/          # 헤더·사이드바·푸터 파셜 (L1/L2/L3 레이아웃)
│   ├── auth/            # 로그인
│   └── error/           # 403, 404
├── static/
│   ├── css/
│   ├── js/
│   └── images/
├── application.properties
├── application-dev.properties
└── application-prod.properties.example
```

---

## 팀 구성 및 담당 영역

| 역할               | 담당 영역                                                 |
| ------------------ | --------------------------------------------------------- |
| 경력자 (Tech Lead) | 아키텍처·Security·Entity·LlmService·배포                  |
| 비전공자 A         | 외부 예약 흐름 (`/reservation/**`)                        |
| 비전공자 B         | 내부 직원 화면 (`/staff/**` · `/doctor/**` · `/nurse/**`) |
| 비전공자 C         | 관리자 화면 (`/admin/**`)                                 |

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
| [doc/PRE_WORK.md](doc/PRE_WORK.md)                                              | 선행 작업 체크리스트                             |
