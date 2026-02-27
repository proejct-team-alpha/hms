# 🏥 병원 예약 & 내부 업무 시스템

> Spring Boot 기반 병원 예약·접수·진료·관리 통합 시스템
> AI 증상 추천 예약 + 병원 규칙 Q&A 챗봇 포함

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security (세션 기반, 4 ROLE) |
| View | Mustache (SSR) |
| ORM | Spring Data JPA |
| DB | MySQL 8.x |
| AI | Claude API (Anthropic) |
| Build | Maven |

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
- Java 17
- MySQL 8.x
- Claude API Key ([Anthropic Console](https://console.anthropic.com) 발급)

### 실행 방법

```bash
# 1. 저장소 클론
git clone https://github.com/[org]/hospital-reservation-system.git
cd hospital-reservation-system

# 2. 환경 설정 파일 생성 (git에 포함되지 않음)
cp src/main/resources/application-example.properties \
   src/main/resources/application-local.properties

# 3. application-local.properties 편집
#    DB 접속 정보 + Claude API Key 입력

# 4. 실행
./mvnw spring-boot:run -Dspring.profiles.active=local
```

### application-local.properties 예시

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/hospital_db
spring.datasource.username=root
spring.datasource.password=your_password

claude.api.key=sk-ant-...
```

> ⚠️ `application-local.properties` 는 `.gitignore` 등록됨 — 절대 커밋 금지

---

## 프로젝트 구조

```
src/main/java/com/hospital/
├── config/          # Security, MVC 설정 (경력자)
├── common/          # 공통 인터셉터, 예외 처리, 유틸
├── domain/          # JPA Entity 전체
├── reservation/     # 외부 예약 흐름
├── staff/           # 접수 직원 화면
├── doctor/          # 의사 화면
├── nurse/           # 간호사 화면
├── admin/           # 관리자 화면 전체
└── llm/             # Claude API 연동

src/main/resources/templates/
├── common/          # 헤더·사이드바·푸터·챗봇 파셜
├── layouts/         # 레이아웃 (L1 비회원 / L2 직원)
├── home/            # 비회원 메인
├── reservation/     # 예약 흐름
├── staff/           # 접수 직원
├── doctor/          # 의사
├── nurse/           # 간호사
└── admin/           # 관리자
```

---

## 팀 구성 및 담당 영역

| 역할 | 담당 영역 |
|------|-----------|
| 경력자 (Tech Lead) | 아키텍처·Security·Entity·LlmService·배포 |
| 비전공자 A | 외부 예약 흐름 (`/reservation/**`) |
| 비전공자 B | 내부 직원 화면 (`/staff/**` · `/doctor/**` · `/nurse/**`) |
| 비전공자 C | 관리자 화면 (`/admin/**`) |

---

## 브랜치 전략

```
main       ← 배포 가능 안정 버전 (경력자 리뷰 필수)
develop    ← 통합 브랜치 (매주 금요일 머지)
feature/*  ← 기능별 개발 브랜치
```

---

## 관련 문서

| 문서 | 버전 |
|------|------|
| 프로젝트 계획서 | v4.2 |
| 화면 정의서 | v1.4 |
| API 명세서 | v2.1 |
| ERD | v2.0 |
| 아키텍처 정의서 | v1.0 |
