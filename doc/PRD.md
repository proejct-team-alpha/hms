# HMS (Hospital Management System) — PRD (Product Requirements Document)

> **문서 버전:** v1.0
> **작성일:** 2026-03-04
> **기준 문서:** 프로젝트 계획서 v4.3 / ERD v4.0 / API 명세서 v5.2 / 화면 기능 정의서 v5.1 / 아키텍처 정의서 v2.0 / PBL 요구사항 정의서 / 개발 일정 총괄표
> **문서 저장소:** [proejct-team-alpha/documents](https://github.com/proejct-team-alpha/documents)

---

## 목차

1. [제품 개요](#1-제품-개요)
2. [목표 및 성공 지표](#2-목표-및-성공-지표)
3. [사용자 및 역할 정의](#3-사용자-및-역할-정의)
4. [기능 요구사항](#4-기능-요구사항)
5. [비기능 요구사항](#5-비기능-요구사항)
6. [PBL 필수 요구사항 매핑](#6-pbl-필수-요구사항-매핑)
7. [기술 스택](#7-기술-스택)
8. [데이터 모델 (ERD)](#8-데이터-모델-erd)
9. [시스템 아키텍처](#9-시스템-아키텍처)
10. [화면 목록 및 흐름](#10-화면-목록-및-흐름)
11. [API 엔드포인트 요약](#11-api-엔드포인트-요약)
12. [개발 일정 및 마일스톤](#12-개발-일정-및-마일스톤)
13. [팀 구성 및 역할 분담](#13-팀-구성-및-역할-분담)
14. [리스크 관리](#14-리스크-관리)
15. [범위 외 항목 (v1.1+)](#15-범위-외-항목-v11)
16. [현재 구현 상태](#16-현재-구현-상태)

---

## 1. 제품 개요

### 1.1 제품명

**HMS (Hospital Management System)** — 병원 예약 & 내부 업무 시스템

### 1.2 제품 비전

비회원 환자가 증상 텍스트를 입력하면 Claude LLM이 적합한 진료과/의사/시간대를 추천하여 예약까지 이어지는 AI 기반 병원 예약 시스템. 내부적으로는 접수-진료-관리까지 역할별 워크플로우를 통합 제공한다.

### 1.3 핵심 가치

| 대상 | 가치 |
|------|------|
| **환자 (비회원)** | AI 증상 분석을 통한 최적 진료과/의사 추천으로 예약 마찰 최소화 |
| **접수 직원** | 당일 접수, 전화/방문 예약 등 통합 접수 워크플로우 |
| **의사** | 진료 대기 목록, 진료 기록 작성 자동화 |
| **간호사** | 환자 정보 관리, 스케줄 확인, 재고 부족 알림 |
| **관리자** | 예약/환자/직원/진료과/물품/규칙 통합 관리 + 대시보드 통계 |
| **의료진 공통** | 병원 규칙 Q&A 챗봇으로 반복 문의 자동화 |

### 1.4 제품 범위 (v1.0 MVP)

- 텍스트 기반 증상 입력 (이미지 제외)
- 완전한 예약 흐름 (중복 방지 포함)
- 4개 역할별 대시보드 및 워크플로우
- 43개 화면 + 89개 API 엔드포인트
- 병원 규칙 CRUD + LLM 챗봇 연동
- 세션 기반 대화 이력 저장

---

## 2. 목표 및 성공 지표

### 2.1 SMART 목표

| 항목 | 내용 |
|------|------|
| **Specific** | 외부 증상 기반 예약 추천 + 규칙 기반 Q&A 챗봇 + 내부 업무 워크플로우 완성 |
| **Measurable** | 4주 내 MVP 완료, LLM 응답 5초 이내, 89개 엔드포인트 구현 |
| **Achievable** | 책임개발자 + 개발자 3인 병렬 개발, AI 보조 활용 |
| **Relevant** | 환자 예약 마찰 감소, 직원 반복 문의 처리 자동화 |
| **Time-bound** | 4주차 종료 시 배포 완료 |

### 2.2 성공 지표

| 지표 | 목표값 |
|------|--------|
| MVP 완성률 | 100% (43개 화면, 89개 API) |
| LLM 증상 분석 응답 시간 | 5초 이내 |
| 예약 중복 방지 | 동일 의사/날짜/시간 중복 0건 |
| PBL 필수 요구사항 충족 | 7개 항목 전체 충족 |
| 역할별 접근 통제 | 무단 접근 0건 |

---

## 3. 사용자 및 역할 정의

### 3.1 외부 사용자

| 역할 | 설명 | 인증 |
|------|------|------|
| **비회원 환자** | 증상 입력 → LLM 추천 → 예약 / 직접 예약 | 불필요 |

### 3.2 내부 사용자 (Staff)

| 역할 | Enum | 접근 경로 | 핵심 업무 |
|------|------|-----------|-----------|
| **관리자** | `ROLE_ADMIN` | `/admin/**` | 전체 시스템 관리, CRUD, 통계 |
| **접수 직원** | `ROLE_STAFF` | `/staff/**` | 접수 처리, 전화/방문 예약 등록 |
| **의사** | `ROLE_DOCTOR` | `/doctor/**` | 진료 완료, 진료 기록 작성 |
| **간호사** | `ROLE_NURSE` | `/nurse/**` | 환자 정보 관리, 스케줄 확인 |

### 3.3 테스트 계정 (개발 환경)

| 아이디 | 비밀번호 | 역할 | 사번 |
|--------|---------|------|------|
| admin01 | password123 | ADMIN | A-20260101 |
| staff01 | password123 | STAFF | S-20260101 |
| doctor01 | password123 | DOCTOR | D-20260101 |
| nurse01 | password123 | NURSE | N-20260101 |

---

## 4. 기능 요구사항

### 4.1 F01 — 비회원 예약 (외부)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F01-1 | 예약 방법 선택 | P0 | AI 추천 예약 / 직접 예약 분기 |
| F01-2 | 증상 입력 | P0 | 텍스트 기반 증상 입력 폼 |
| F01-3 | LLM 증상 분석 | P0 | Claude API로 증상 분석 → 진료과/의사/시간 추천 |
| F01-4 | 직접 예약 | P0 | 진료과 → 의사 → 날짜/시간 순차 선택 |
| F01-5 | 예약 생성 | P0 | 환자 정보 입력 + 중복 검증 + 예약번호 채번 |
| F01-6 | 예약 확인 | P0 | 예약번호, 의사, 날짜/시간 확인 화면 |

**비즈니스 규칙:**
- 예약번호 형식: `RES-YYYYMMDD-NNN`
- 동일 의사/날짜/시간 중복 예약 불가 (UNIQUE 제약)
- 의사 진료 가능 요일 검증 (`available_days`)
- 30분 단위 시간 슬롯 (09:00~17:30)
- LLM 실패 시 수동 선택으로 자동 전환 (5초 타임아웃)
- 의료 면책 조항 필수 표시

### 4.2 F02 — 인증

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F02-1 | 로그인 | P0 | Spring Security 세션 기반, BCrypt 패스워드 |
| F02-2 | 역할별 리다이렉트 | P0 | 로그인 성공 시 역할별 대시보드로 이동 |
| F02-3 | 로그아웃 | P0 | 세션 무효화 + 로그인 페이지 리다이렉트 |
| F02-4 | 접근 제어 | P0 | URL 패턴별 역할 기반 접근 통제 |

**접근 권한 매트릭스:**

| URL 패턴 | 접근 권한 |
|----------|----------|
| `/`, `/reservation/**` | 전체 공개 |
| `/llm/symptom/**` | 전체 공개 |
| `/login`, `/logout` | 전체 공개 |
| `/staff/**` | STAFF, ADMIN |
| `/doctor/**` | DOCTOR, ADMIN |
| `/nurse/**` | NURSE, ADMIN |
| `/admin/**` | ADMIN |
| `/llm/rules/**` | DOCTOR, NURSE |
| 기타 | 인증 필요 |

### 4.3 F03 — 접수 직원 업무

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F03-1 | 접수 대시보드 | P0 | 당일 예약 현황 요약 |
| F03-2 | 접수 처리 | P0 | RESERVED → RECEIVED 상태 전이 + 환자 주소/메모 입력 |
| F03-3 | 전화 예약 등록 | P0 | 직원이 환자 대신 예약 생성 (source: PHONE) |
| F03-4 | 방문 접수 | P0 | 워크인 환자 즉시 접수 (source: WALKIN) |
| F03-5 | 내 정보 관리 | P1 | 비밀번호 변경, 개인정보 수정 |

### 4.4 F04 — 의사 업무

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F04-1 | 의사 대시보드 | P0 | 당일 진료 대기 목록 |
| F04-2 | 진료 목록 | P0 | RECEIVED 상태 환자 목록 |
| F04-3 | 진료 상세/완료 | P0 | 진단명, 처방, 비고 입력 → RECEIVED → COMPLETED 전이 |
| F04-4 | 내 정보 관리 | P1 | 비밀번호 변경, 개인정보 수정 |

### 4.5 F05 — 간호사 업무

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F05-1 | 간호사 대시보드 | P0 | 현황 요약 + 재고 부족 알림 |
| F05-2 | 스케줄 목록 | P0 | 실시간 예약 상태 확인 |
| F05-3 | 환자 정보 조회/수정 | P0 | 환자 상세 확인 및 정보 업데이트 |
| F05-4 | 내 정보 관리 | P1 | 비밀번호 변경, 개인정보 수정 |

### 4.6 F06 — LLM 통합

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F06-1 | 증상 분석 | P0 | 환자 증상 텍스트 → Claude API → 진료과/의사/시간 추천 |
| F06-2 | 규칙 Q&A 챗봇 | P0 | 의사/간호사가 병원 규칙 질문 → LLM 답변 |
| F06-3 | 대화 이력 | P1 | 세션별 챗봇 대화 이력 저장 및 조회 |

**LLM 기술 사양:**
- 모델: Claude Sonnet
- 타임아웃: 5초
- 호출 방식: 서버 사이드 전용 (RestClient)
- 증상 분석 실패 시: 수동 선택 폴백
- 추천 이력: `LLM_RECOMMENDATION` 테이블에 저장, 예약 전환 추적 (`is_used`)

### 4.7 F07 — 관리자 — 예약/접수 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F07-1 | 접수 현황 목록 | P0 | 당일 접수 통합 뷰 (소스 라벨: ONLINE/PHONE/WALKIN) |
| F07-2 | 예약 목록 | P0 | 날짜 범위, 상태, 진료과별 필터링 |
| F07-3 | 예약 취소 | P0 | COMPLETED 상태 예약은 취소 불가 |

### 4.8 F08 — 관리자 — 환자 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F08-1 | 환자 목록 | P0 | 검색 가능한 환자 디렉토리 |
| F08-2 | 환자 상세 | P0 | 프로필 + 전체 예약 이력 |

### 4.9 F09 — 관리자 — 인사 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F09-1 | 직원 목록 | P0 | 역할별 필터링 가능한 직원 디렉토리 |
| F09-2 | 직원 등록 | P0 | 신규 직원 생성 (의사 시 DOCTOR 레코드 자동 생성) |
| F09-3 | 직원 상세/수정 | P0 | 직원 정보 수정 |
| F09-4 | 직원 비활성화 | P0 | 삭제 대신 비활성화 처리 |

### 4.10 F10 — 관리자 — 진료과 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F10-1 | 진료과 목록 | P0 | 활성/비활성 목록 |
| F10-2 | 진료과 등록 | P0 | 인라인 등록 |
| F10-3 | 진료과 상세 | P0 | 소속 의사 목록 + 통계 |
| F10-4 | 진료과 수정 | P0 | 이름/상태 변경 |
| F10-5 | 진료과 활성화/비활성화 | P0 | 비활성화 시 예약/LLM 프롬프트에서 제외 |

### 4.11 F11 — 관리자 — 물품 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F11-1 | 물품 목록 | P0 | 재고 상태별 필터링 |
| F11-2 | 물품 등록 | P0 | 이름, 카테고리, 수량, 최소 수량 임계값 |
| F11-3 | 물품 상세/수정 | P0 | 물품 정보 수정 |
| F11-4 | 물품 카테고리 CRUD | P1 | 카테고리 동적 관리 (활성화/비활성화) |

**물품 카테고리:** MEDICAL_SUPPLIES / MEDICAL_EQUIPMENT / GENERAL_SUPPLIES (+ 동적 추가 가능)

### 4.12 F12 — 관리자 — 병원 규칙 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F12-1 | 규칙 목록 | P0 | 카테고리, 상태, 제목별 검색 |
| F12-2 | 규칙 등록 | P0 | 제목, 카테고리, 내용 (3000자 제한) |
| F12-3 | 규칙 상세/수정 | P0 | 규칙 내용 수정 |
| F12-4 | 규칙 활성화 토글 | P0 | 즉시 챗봇 컨텍스트 반영 |
| F12-5 | 규칙 삭제 | P1 | 영구 삭제 |
| F12-6 | 규칙 카테고리 CRUD | P1 | 카테고리 동적 관리 |

**규칙 카테고리:** EMERGENCY / SUPPLY / DUTY / HYGIENE / OTHER (+ 동적 추가 가능)

### 4.13 F13 — 관리자 — 대시보드

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| F13-1 | 대시보드 화면 | P0 | 5개 핵심 통계 렌더링 |
| F13-2 | 실시간 통계 | P1 | AJAX 기반 통계 갱신 |

**대시보드 통계 항목:**
1. 오늘 예약 건수
2. 전체 예약 건수
3. 진료과별 예약 분포
4. 재고 부족 물품 수
5. 직원 수

### 4.14 F14 — JSON API 레이어

PBL REST API 필수 요구사항 충족용 `/api/**` 엔드포인트:

| ID | 메서드 | 경로 | 설명 |
|----|--------|------|------|
| F14-1 | POST | `/api/staff/{id}/update` | 의사 전문분야/진료요일 수정 |
| F14-2 | POST | `/api/patients/{id}/update` | 환자 정보 수정 |
| F14-3 | POST | `/api/reservations/{id}/cancel` | 예약 취소 |
| F14-4 | POST | `/api/items/{id}/delete` | 물품 삭제 |
| F14-5 | POST | `/api/rules/{id}/delete` | 규칙 삭제 |

**응답 형식:** `{ success: boolean, data/errorCode: any, message: string }`

---

## 5. 비기능 요구사항

| 분류 | 요구사항 | 목표 |
|------|----------|------|
| **성능** | LLM 응답 시간 | 5초 이내 (타임아웃 후 폴백) |
| **성능** | 페이지 렌더링 | SSR 기반 빠른 초기 로드 |
| **보안** | 인증/인가 | Spring Security 세션 기반, 역할별 URL 접근 통제 |
| **보안** | CSRF 방어 | 모든 POST 폼에 CSRF 토큰 포함 |
| **보안** | API Key 보호 | 환경 변수 전용, 서버 사이드 호출만 허용, .gitignore 적용 |
| **보안** | 비밀번호 | BCrypt 암호화 저장 |
| **가용성** | LLM 장애 대응 | 수동 선택 폴백 상시 제공 |
| **데이터 무결성** | 중복 예약 방지 | DB UNIQUE 제약 + 트랜잭션 검증 |
| **데이터 무결성** | 상태 전이 제어 | 도메인 메서드 기반 상태 전이만 허용 |
| **유효성 검증** | 서버 사이드 | `@Valid` + `BindingResult` 전 POST 엔드포인트 |
| **페이징** | 목록 조회 | `Pageable` + `@PageableDefault(size=20)` |
| **에러 처리** | 전역 예외 | `@ControllerAdvice` + `CustomException` 팩토리 |

---

## 6. PBL 필수 요구사항 매핑

| # | PBL 요구사항 | 시스템 구현 | 충족 여부 |
|---|-------------|------------|-----------|
| 1 | REST API 3개 이상 (GET/POST/PUT/DELETE + AJAX) | `/api/**` 레이어 5개 엔드포인트 + AJAX 호출 | O |
| 2 | 엔티티 4개 이상 + JPA 관계 매핑 | 12개 엔티티 + @OneToMany/@ManyToOne 다수 | O |
| 3 | 인증/인가 + 2개 이상 역할 | Spring Security 4개 역할 (ADMIN/DOCTOR/NURSE/STAFF) | O |
| 4 | Redis 세션 관리 | spring-session-data-redis 적용 | O |
| 5 | Mustache SSR | 전체 43개 화면 Mustache 렌더링 | O |
| 6 | 전역 예외 처리 + @Valid | GlobalExceptionHandler + DTO @Valid 검증 | O |
| 7 | 페이징 + 정렬 | Pageable + @PageableDefault(size=20) | O |

---

## 7. 기술 스택

| 분류 | 기술 | 용도 |
|------|------|------|
| **언어** | Java 21 | 백엔드 |
| **프레임워크** | Spring Boot 4.0.3 | 웹 애플리케이션 |
| **뷰 엔진** | Mustache | SSR (서버 사이드 렌더링) |
| **프론트엔드** | Vanilla JS | AJAX, 동적 UI |
| **ORM** | Spring Data JPA / Hibernate | 데이터 접근 |
| **DB (개발)** | H2 in-memory (MODE=MySQL) | 개발 환경 |
| **DB (운영)** | MySQL 8.x | 운영 환경 |
| **세션** | Redis + Spring Session | 세션 관리 (운영) |
| **인증** | Spring Security | 세션 기반 인증/인가 |
| **LLM** | Claude API (Sonnet) | 증상 분석, 규칙 챗봇 |
| **HTTP 클라이언트** | RestClient | Claude API 호출 |
| **빌드** | Gradle | 의존성 관리, 빌드 |
| **유틸리티** | Lombok | 보일러플레이트 제거 |
| **JWT** | com.auth0:java-jwt:4.3.0 | 선택적 사용 |
| **테스트** | JUnit5 + MockMvc | 단위/컨트롤러 테스트 |
| **문서화** | Spring REST Docs | API 문서 자동 생성 |

---

## 8. 데이터 모델 (ERD)

### 8.1 엔티티 관계도

```
Department (진료과)
  │
  ├── 1:N ── Staff (직원)     ──── StaffRole: ADMIN | DOCTOR | NURSE | STAFF
  │            │
  │            └── 1:1 ── Doctor (의사 상세)
  │                          │  available_days, specialty
  │                          │
  │                          ├── 1:N ── Reservation (예약)
  │                          │            │  reservationNumber, date, timeSlot
  │                          │            │  status: RESERVED → RECEIVED → COMPLETED / CANCELLED
  │                          │            │  source: ONLINE | PHONE | WALKIN
  │                          │            │
  │                          │            ├── N:1 ── Patient (환자)
  │                          │            │            name, phone, email, birthDate, gender
  │                          │            │
  │                          │            └── 1:1 ── TreatmentRecord (진료기록)
  │                          │                         diagnosis, prescription, remark
  │                          │
  │                          └── 1:N ── TreatmentRecord
  │
  └── 1:N ── Doctor

Item (물품)              ──── ItemCategory (물품 카테고리)
HospitalRule (병원규칙)  ──── HospitalRuleCategory (규칙 카테고리)
LlmRecommendation (LLM 추천)  ──── symptomText, recommendedDept/Doctor/Time, isUsed
ChatbotHistory (챗봇 이력)     ──── sessionId, Staff(FK), question, answer
```

### 8.2 핵심 엔티티 목록

| # | 엔티티 | 설명 | 주요 필드 |
|---|--------|------|-----------|
| 1 | Patient | 비회원 환자 | name, phone, birthDate, gender, email, address, notes |
| 2 | Staff | 내부 직원 (로그인 계정) | username, employeeNumber, password, role, departmentId |
| 3 | Doctor | 의사 상세 (Staff 1:1) | staffId, specialty, availableDays |
| 4 | Department | 진료과 | name, isActive |
| 5 | Reservation | 예약 | reservationNumber, doctorId, patientId, date, timeSlot, status, source |
| 6 | ReservationStatus | enum | RESERVED, RECEIVED, COMPLETED, CANCELLED |
| 7 | ReservationSource | enum | ONLINE, PHONE, WALKIN |
| 8 | TreatmentRecord | 진료 기록 | reservationId, doctorId, diagnosis, prescription, remark |
| 9 | Item | 물품 | name, categoryId, quantity, minQuantity |
| 10 | ItemCategory | 물품 카테고리 | name, isActive |
| 11 | HospitalRule | 병원 규칙 | title, categoryId, content(3000자), isActive |
| 12 | HospitalRuleCategory | 규칙 카테고리 | name, isActive |
| 13 | LlmRecommendation | LLM 추천 이력 | symptomText, recommendedDept, recommendedDoctor, recommendedTime, isUsed |
| 14 | ChatbotHistory | 챗봇 대화 이력 | sessionId, staffId, question, answer |

### 8.3 상태 전이 규칙

```
RESERVED ──(접수 처리)──→ RECEIVED ──(진료 완료)──→ COMPLETED
    │                        │
    └──(취소)──→ CANCELLED ←─┘

※ COMPLETED / CANCELLED → 다른 상태 전이 불가
```

| 전이 | 실행 역할 | 트리거 |
|------|----------|--------|
| RESERVED → RECEIVED | STAFF | 접수 처리 (POST /staff/reception/receive) |
| RECEIVED → COMPLETED | DOCTOR | 진료 완료 (POST /doctor/treatment/complete) |
| RESERVED → CANCELLED | ADMIN | 예약 취소 (POST /admin/reservation/cancel) |
| RECEIVED → CANCELLED | ADMIN | 예약 취소 (POST /admin/reservation/cancel) |

---

## 9. 시스템 아키텍처

### 9.1 레이어 구조

```
┌─────────────────────────────────────────────────┐
│  Security Layer (Spring Security)               │
│  - CSRF, 역할별 URL 접근, 세션 관리             │
├─────────────────────────────────────────────────┤
│  Controller Layer                               │
│  - SSR: GET → Mustache 뷰 반환                  │
│  - POST → PRG 리다이렉트 / AJAX JSON 응답       │
├─────────────────────────────────────────────────┤
│  Service Layer                                  │
│  - 비즈니스 로직, 검증, 상태 전이               │
│  - @Transactional(readOnly=true) 클래스 레벨    │
├─────────────────────────────────────────────────┤
│  Repository Layer (Spring Data JPA)             │
│  - 파생 쿼리, JPQL, 네이티브 SQL                │
├─────────────────────────────────────────────────┤
│  Persistence Layer                              │
│  - Entity, H2(dev) / MySQL(prod), Redis 세션    │
└─────────────────────────────────────────────────┘
```

### 9.2 컨트롤러 반환 패턴

| 유형 | 패턴 | 반환값 |
|------|------|--------|
| **GET 화면** | `request.setAttribute(key, val)` | `"역할/자원/액션"` (뷰 경로) |
| **POST 성공** | `RedirectAttributes.addFlashAttribute` | `"redirect:/다음화면"` (PRG 패턴) |
| **POST 실패** | `request.setAttribute("errorCode", ...)` | 원래 폼 뷰 경로 (재렌더링) |
| **AJAX** | `@ResponseBody` | `{ success, data, message }` JSON |

### 9.3 공통 모델 인터셉터 (LayoutModelInterceptor)

모든 뷰에 자동 주입되는 변수:

| 변수명 | 설명 |
|--------|------|
| `pageTitle` | 페이지 제목 |
| `currentPath` | 현재 요청 URI |
| `loginName` | 로그인 사용자 이름 |
| `isAdmin` / `isDoctor` / `isNurse` / `isStaff` | 역할 플래그 |
| `showChatbot` | 챗봇 표시 여부 (DOCTOR/NURSE) |
| `dashboardUrl` | 역할별 대시보드 경로 |

### 9.4 예외 처리

| 팩토리 메서드 | HTTP | 용도 |
|--------------|------|------|
| `notFound(msg)` | 404 | 리소스 없음 |
| `badRequest(code, msg)` | 400 | 잘못된 요청 |
| `unauthorized(msg)` | 401 | 인증 실패 |
| `forbidden(msg)` | 403 | 권한 없음 |
| `conflict(code, msg)` | 409 | 중복/충돌 |
| `invalidStatusTransition(msg)` | 409 | 상태 전이 오류 |
| `serviceUnavailable(msg)` | 503 | LLM 서비스 장애 |

---

## 10. 화면 목록 및 흐름

### 10.1 레이아웃 시스템

| 레이아웃 | 구조 | 대상 |
|----------|------|------|
| L1 (공개) | header-public → main → footer-public | 비회원 화면 |
| L2 (직원) | header-staff → sidebar-{role} + main → footer-staff | 내부 직원 |
| L3 (로그인) | header-login → main.login-main | 로그인 화면 |

### 10.2 전체 화면 목록 (43개 + 챗봇 1개)

| 구분 | 화면 ID | 화면명 | URL | 역할 |
|------|---------|--------|-----|------|
| **비회원** | S00 | 메인 (방법 선택) | `/reservation` | 공개 |
| | S01 | 증상 입력 | `/reservation/symptom` | 공개 |
| | S02 | LLM 추천 결과 | (AJAX 영역) | 공개 |
| | S03 | 직접 예약 | `/reservation/direct` | 공개 |
| | S04 | 예약 확인 | `/reservation/confirm` | 공개 |
| **인증** | S05 | 로그인 | `/login` | 공개 |
| **접수** | S06 | 접수 대시보드 | `/staff/dashboard` | STAFF |
| | S07 | 접수 목록 | `/staff/reception/list` | STAFF |
| | S08 | 접수 처리 | `/staff/reception/detail` | STAFF |
| | S09 | 전화 예약 | `/staff/reservation/new` | STAFF |
| | S10 | 방문 접수 | `/staff/walkin/new` | STAFF |
| | S33 | 내 정보 관리 | `/staff/mypage` | STAFF |
| **의사** | S11 | 의사 대시보드 | `/doctor/dashboard` | DOCTOR |
| | S12 | 진료 목록 | `/doctor/treatment/list` | DOCTOR |
| | S13 | 진료 상세/완료 | `/doctor/treatment/detail` | DOCTOR |
| | S34 | 내 정보 관리 | `/doctor/mypage` | DOCTOR |
| **간호사** | S14 | 간호사 대시보드 | `/nurse/dashboard` | NURSE |
| | S15 | 스케줄 목록 | `/nurse/schedule/list` | NURSE |
| | S16 | 환자 상세/수정 | `/nurse/patient/detail` | NURSE |
| | S35 | 내 정보 관리 | `/nurse/mypage` | NURSE |
| **관리자** | S17 | 관리자 대시보드 | `/admin/dashboard` | ADMIN |
| | S18 | 접수 현황 | `/admin/reception/list` | ADMIN |
| | S19 | 예약 목록 | `/admin/reservation/list` | ADMIN |
| | S20 | 환자 목록 | `/admin/patient/list` | ADMIN |
| | S21 | 환자 상세 | `/admin/patient/detail` | ADMIN |
| | S22 | 직원 목록 | `/admin/staff/list` | ADMIN |
| | S23 | 직원 등록 | `/admin/staff/new` | ADMIN |
| | S24 | 직원 상세/수정 | `/admin/staff/detail` | ADMIN |
| | S25 | 진료과 목록 | `/admin/department/list` | ADMIN |
| | S26 | 진료과 상세 | `/admin/department/detail` | ADMIN |
| | S27 | 물품 목록 | `/admin/item/list` | ADMIN |
| | S28 | 물품 등록 | `/admin/item/new` | ADMIN |
| | S29 | 물품 상세/수정 | `/admin/item/detail` | ADMIN |
| | S30 | 규칙 목록 | `/admin/rule/list` | ADMIN |
| | S31 | 규칙 등록 | `/admin/rule/new` | ADMIN |
| | S32 | 규칙 상세/수정 | `/admin/rule/detail` | ADMIN |
| | S36 | 내 정보 관리 | `/admin/mypage` | ADMIN |
| | S37-42 | 카테고리 관리 | `/admin/item-category/**`, `/admin/rule-category/**` | ADMIN |
| **공통** | C01 | 규칙 챗봇 | (오버레이) | DOCTOR, NURSE |

### 10.3 핵심 사용자 플로우

**환자 예약 플로우:**
```
S00 방법선택 → [AI 추천] → S01 증상입력 → S02 LLM분석(AJAX) → S03 예약폼(프리필) → S04 확인
                         → [직접 예약] → S03 예약폼(수동선택) → S04 확인
```

**접수 → 진료 플로우:**
```
S07 접수목록 → S08 접수처리(RESERVED→RECEIVED)
  → S12 진료목록(DOCTOR) → S13 진료완료(RECEIVED→COMPLETED)
```

**관리자 규칙 → 챗봇 플로우:**
```
S30 규칙목록 → S31 규칙등록 → 활성화 → C01 챗봇에서 즉시 반영
```

---

## 11. API 엔드포인트 요약

### 11.1 전체 현황: 89개 엔드포인트

| 모듈 | GET | POST | 소계 |
|------|-----|------|------|
| 인증 | 1 | 0 | 1 |
| 비회원 예약 | 4 | 1 | 5 |
| LLM 증상 분석 | 0 | 1 | 1 |
| LLM 규칙 챗봇 | 1 | 1 | 2 |
| 접수 직원 (STAFF) | 7 | 5 | 12 |
| 의사 (DOCTOR) | 4 | 2 | 6 |
| 간호사 (NURSE) | 4 | 2 | 6 |
| 관리자 — 예약/접수 | 3 | 1 | 4 |
| 관리자 — 환자 | 2 | 0 | 2 |
| 관리자 — 인사 | 3 | 3 | 6 |
| 관리자 — 진료과 | 2 | 5 | 7 |
| 관리자 — 물품 | 3 | 3 | 6 |
| 관리자 — 물품 카테고리 | 3 | 4 | 7 |
| 관리자 — 규칙 | 3 | 4 | 7 |
| 관리자 — 규칙 카테고리 | 3 | 4 | 7 |
| 관리자 — 대시보드 | 2 | 0 | 2 |
| JSON API (/api/**) | 0 | 5 | 5 |
| AJAX 보조 | 2 | 0 | 2 |
| **합계** | **46** | **40** | **89** |

### 11.2 주요 AJAX 엔드포인트

| 경로 | 설명 |
|------|------|
| `GET /reservation/getDoctors?departmentId=` | 진료과별 의사 목록 |
| `GET /reservation/getSlots?doctorId=&date=` | 의사별 가용 시간 슬롯 |
| `POST /llm/symptom/analyze` | 증상 분석 (LLM) |
| `POST /llm/rules/ask` | 규칙 챗봇 질문 |
| `GET /admin/dashboard/stats` | 대시보드 통계 JSON |

### 11.3 에러 코드

```
DUPLICATE_RESERVATION       INVALID_TIME_SLOT          DOCTOR_NOT_AVAILABLE
RESERVATION_NOT_FOUND       CANNOT_CANCEL_COMPLETED    INVALID_STATUS_TRANSITION
ALREADY_CANCELLED           ALREADY_COMPLETED          UNAUTHORIZED
ACCESS_DENIED               NOT_OWN_PATIENT            LLM_SERVICE_UNAVAILABLE
LLM_PARSE_ERROR             RESOURCE_NOT_FOUND         DUPLICATE_USERNAME
VALIDATION_ERROR
```

---

## 12. 개발 일정 및 마일스톤

### 12.1 4주 스프린트

| 주차 | 마일스톤 | 핵심 활동 | 산출물 |
|------|----------|-----------|--------|
| **W1** | v0.1 기반 구축 | ERD 설계, 인증 시스템 (4 ROLE), UI 템플릿, Entity 완성 | 인증 동작, 4개 대시보드 접근 |
| **W2** | v0.5 예약 흐름 | 예약 폼, 슬롯 선택, 중복 방지, 접수 처리, SlotService | 예약 → 접수 플로우 완성 |
| **W3** | v0.8 직원 시스템 | 진료 기록, 관리자 CRUD, LlmService 프레임워크 착수 | 전체 CRUD + 진료 완료 |
| **W4** | v1.0 MVP | Claude API 연동, 챗봇 UI, 통합 테스트, 배포 | 전체 기능 동작 |

### 12.2 핵심 의존성

```
Entity 완성 (W1) ──→ 전 개발자 병렬 작업 시작 (W2)
SlotService 완성 (W2) ──→ 예약/접수 워크플로우 (W2-W3)
LlmService 완성 (W3 수요일) ──→ 증상 분석 UI + 챗봇 UI 연결 (W4)
```

### 12.3 커뮤니케이션

- 일일 스탠드업: 매일 오전 10:00
- 주간 회의: 매주 금요일 오후 2:00

---

## 13. 팀 구성 및 역할 분담

### 13.1 개발 소유권 (Vertical Ownership)

| 담당자 | 소유 영역 | URL 범위 | 핵심 책임 |
|--------|----------|----------|-----------|
| **책임개발자** | 설정/공통/Entity/LLM | config, common, domain, llm | 아키텍처, 보안, Entity, SlotService, LlmService, 코드 리뷰, 배포 |
| **개발자 A** | 비회원 예약 | `/reservation/**` | 예약 폼, 증상 입력 UI, LLM 추천 결과 UI |
| **개발자 B** | 내부 직원 | `/staff/**`, `/doctor/**`, `/nurse/**` | 접수 처리, 진료 완료, 간호 업무, 챗봇 UI |
| **개발자 C** | 관리자 | `/admin/**` | 전체 관리자 CRUD, 대시보드, 물품/규칙 관리 |

### 13.2 충돌 방지 규칙

1. **수직 소유** — Controller → Service → Repository 스택 단위 소유
2. **공유 파일 잠금** — Entity, SecurityConfig, SlotService는 책임개발자만 수정
3. **Repository 경계** — 타 기능 조회는 허용, 쓰기는 소유자 Service를 통해서만
4. **상태 전이 독점** — 각 상태 전이는 하나의 Service만 처리
5. **템플릿 디렉토리 분리** — `/reservation/**`(A), `/staff|doctor|nurse/**`(B), `/admin/**`(C), `/common/**`(책임)

---

## 14. 리스크 관리

| 리스크 | 영향도 | 발생확률 | 대응 전략 |
|--------|--------|----------|-----------|
| W3 LlmService 지연 | 높음 | 중간 | 챗봇 없이 증상 추천만 배포 가능 |
| Claude API 장애 | 높음 | 낮음 | 5초 타임아웃 + 수동 선택 폴백 상시 제공 |
| LLM 부정확 추천 | 중간 | 중간 | 의료 면책 조항 필수 + "직접 선택" 옵션 항상 표시 |
| API Key 노출 | 높음 | 낮음 | 환경 변수 전용, 서버 사이드 호출, .gitignore |
| 4주 범위 초과 | 높음 | 중간 | Scope Lock 적용 — 모든 신규 기능은 v1.1 이후 |
| 파일 충돌 | 중간 | 중간 | Vertical Ownership + 공유 파일 잠금 규칙 |
| 중복 예약 | 높음 | 낮음 | DB UNIQUE 제약 + SlotService 트랜잭션 검증 |

---

## 15. 범위 외 항목 (v1.1+)

| 항목 | 설명 | 우선순위 |
|------|------|----------|
| 이미지 증상 업로드 | 사진 기반 증상 분석 | v1.1 |
| LLM 피드백 수집 | 추천 정확도 평가 | v1.1 |
| 직원용 챗봇 확장 | 접수 직원/관리자 챗봇 | v1.1 |
| RAG / 벡터 DB | 규칙 검색 고도화 | v1.2 |
| SMS/이메일 알림 | 예약 확인 알림 | v1.2 |
| 실시간 SSE 알림 | 서버 푸시 이벤트 | v1.2 |
| 파일 업로드 | 환자 서류 첨부 | v1.2 |
| 이메일 인증 | 직원 가입 이메일 검증 | v1.2 |

---

## 16. 현재 구현 상태

### 16.1 구현 완료

| 구분 | 항목 |
|------|------|
| **config** | SecurityConfig, WebMvcConfig, ClaudeApiConfig, ErrorPageController |
| **common** | CustomException, GlobalExceptionHandler, LayoutModelInterceptor, ReservationNumberGenerator |
| **domain** | 10개 엔티티 + 5개 enum (Patient, Staff, Doctor, Department, Reservation, TreatmentRecord, Item, HospitalRule, LlmRecommendation, ChatbotHistory) |
| **auth** | AuthController (GET /login) |
| **dashboard** | Admin, Staff, Doctor, Nurse 각 대시보드 컨트롤러 + Mustache |
| **templates** | login, 403/404, 공통 header/footer/sidebar (9개), 대시보드 4개 |
| **설정** | application.properties, application-dev.properties, sql_test.sql |
| **참고** | _sample 패키지 (Entity/Controller/Service/Repository/DTO 패턴 가이드) |

### 16.2 미구현 (개발 필요)

| 구분 | 필요 항목 | 관련 기능 |
|------|----------|-----------|
| **Repository** | StaffRepository, DoctorRepository, ReservationRepository, PatientRepository 등 | 전체 |
| **common/service** | SlotService (30분 슬롯 생성, 의사 진료요일 검증) | F01 |
| **common/service** | ReservationValidationService (중복 예약 검증) | F01, F03 |
| **auth** | StaffUserDetailsService (DB 기반 인증) | F02 |
| **reservation** | ReservationController, ReservationService | F01 |
| **staff** | ReceptionController/Service, 예약 등록, 당일 접수 | F03 |
| **doctor** | TreatmentController/Service | F04 |
| **nurse** | 환자 정보 조회/수정, 스케줄 확인 | F05 |
| **admin** | 예약/환자/직원/진료과/물품/규칙 관리, 대시보드 통계 | F07~F13 |
| **llm** | LlmService, 증상 분석, 규칙 챗봇 | F06 |
| **api** | JSON API 레이어 (/api/**) | F14 |
| **templates** | 각 모듈별 목록/상세/등록/수정 화면 | 전체 |

---

> **참고:** 이 PRD는 [proejct-team-alpha/documents](https://github.com/proejct-team-alpha/documents) 저장소의 공식 문서들과 프로젝트 내 [doc/PROJECT_STRUCTURE.md](doc/PROJECT_STRUCTURE.md)를 기반으로 작성되었습니다. 상세 API 명세는 [doc/API.md](doc/API.md)를, 코딩 규칙은 [doc/RULE.md](doc/RULE.md) 및 [doc/rules/](doc/rules/) 디렉토리를 참고하세요.
