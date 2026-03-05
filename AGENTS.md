# HMS Development Agent Guide

## 0) AI Context Files

Agents must read these files before starting work.

1. `AGENTS.md`
2. `.ai/memory.md`
3. `doc/PROJECT_STRUCTURE.md`
4. `doc/RULE.md`

## 1) 목적

- 이 문서는 HMS 프로젝트에서 AI/자동화 에이전트가 개발할 때 따라야 할 기준 문서, 구현 순서, 품질 규칙을 정의한다.

## 2) 필수 참조 문서

- 로컬 규칙: `doc/RULE.md`
- 로컬 구조 기준: `doc/PROJECT_STRUCTURE.md`
- 외부 문서 저장소: `https://github.com/proejct-team-alpha/documents`
  이 저장소는 HMS 시스템의 공식 문서 저장소이며 API/화면/아키텍처 기준 문서로 사용한다.

## 3) 문서 우선순위

1. 현재 코드와 직접 연결되는 로컬 문서(`doc/PROJECT_STRUCTURE.md`, `doc/RULE.md`)를 1순위로 따른다.
2. 기능 요구사항, 화면/API 상세는 외부 문서 저장소 최신 권장 버전을 따른다.
3. 충돌 시 판단 원칙:

- 코드 구조/패키지/실제 파일 경로 충돌: 로컬 `doc/PROJECT_STRUCTURE.md` 우선
- 코딩 스타일/테스트/예외 처리 충돌: 로컬 `doc/RULE.md` 우선
- 기능 스펙/API/화면 흐름 충돌: 외부 문서 저장소 최신 권장 버전 우선

## 4) 외부 문서 저장소 적용 기준

- 기준 저장소: `proejct-team-alpha/documents` (branch: `master`)
- 우선 활용 문서:
- API 명세서: `04_API_명세서` v5.2 권장
- 화면 기능 정의서: `06_화면_기능_정의서` v5.1 권장
- 화면 흐름 시퀀스: `05_화면_흐름_시퀀스_다이어그램` v5.0 권장
- 아키텍처 정의서: `03_프로젝트_아키텍처_정의서` v2.0 권장
- 보조 기준: `11_테스트_전략서`, `12_코드_리뷰_규칙(v2)`, `13_배포_가이드`
- 버전 표기가 여러 개인 경우 README에서 "권장"으로 명시된 최신 버전을 사용한다.

## 5) 구현 원칙 (로컬 RULE.md 반영)

- Spring
- `@Transactional(readOnly = true)` 기본, 쓰기 메서드만 별도 트랜잭션
- DTO는 Record 우선, 입력은 `@Valid`/`@Validated` 검증
- 예외는 `CustomException` + `GlobalExceptionHandler` 포맷 유지
- RESTful URI와 계층형 자원 표현 유지
- Test
- Given-When-Then 구조 필수
- 핵심 비즈니스 로직 테스트 없이 배포 금지
- 시간/랜덤/외부의존은 결정성 있게 고정 또는 Mock 처리
- Front
- JavaScript: `const` 우선, `var` 금지, 비동기는 async/await 중심
- CSS: BEM, 셀렉터 깊이 최소화, `!important`/id 셀렉터 지양

## 6) 구조 원칙 (로컬 PROJECT_STRUCTURE.md 반영)

- 루트 패키지: `com.smartclinic.hms`
- 모듈 분리 유지: `auth`, `admin`, `staff`, `doctor`, `nurse`, `reservation`, `llm`, `common`, `config`
- 공통 처리
- 예외: `common/exception`
- 인터셉터: `common/interceptor`
- 유틸/서비스: `common/util`, `common/service`
- 템플릿은 역할 기반 레이아웃 패턴(L1/L2/L3)을 유지한다.

## 7) 작업 절차

1. `AGENTS.md`와 `.ai/memory.md`를 먼저 읽어 프로젝트 규칙을 이해한다.
2. 작업 시작 전 `doc/PROJECT_STRUCTURE.md`에서 대상 모듈/경로 확인
3. `doc/RULE.md`에서 적용 규칙(백엔드/테스트/프론트/CSS) 확인
4. 외부 문서 저장소에서 해당 기능의 API/화면/시퀀스 최신 권장 버전 확인
5. 구현
6. 테스트 코드 작성 또는 업데이트
7. 변경 요약 시 "참조 문서"와 "적용한 버전"을 명시

## 8) 금지 사항

- 기존 URL prefix(`/admin`, `/staff`, `/doctor`, `/nurse`, `/reservation`)를 임의 변경하지 않는다.
- 로컬 구조 문서와 다른 임의 패키지/레이어 생성 금지
- 기존 모듈 구조 외 새로운 모듈(예: management, admin2 등) 생성 금지
- 규칙 문서에 반하는 코딩 패턴(예: `var`, 무분별한 then/catch, id 셀렉터 남용) 금지
- 테스트 없이 핵심 로직 반영 금지
- 민감정보 하드코딩 금지 (`.env`, 환경변수 사용)

## 9) 작업 산출물 보고 포맷

- 변경 파일 목록
- 구현 기능 요약
- 참조 문서
- 로컬: `doc/RULE.md`, `doc/PROJECT_STRUCTURE.md`
- 외부: `documents` 저장소 내 사용 문서명/버전
- 검증 결과: 실행한 테스트/검증 범위
- 남은 TODO/리스크
