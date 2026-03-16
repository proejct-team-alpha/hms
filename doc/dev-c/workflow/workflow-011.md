# 직원 목록 SSR 구현 명세서 (workflow-011)

## 문제 정의
관리자 직원 목록 페이지를 SSR로 구현한다. `GET /admin/staff/list`에서 DB 연동 기반으로 직원 목록을 조회하고, 키워드 검색과 역할/재직 상태 필터, 페이징을 지원한다.

## 대상 사용자
- 관리자(`ROLE_ADMIN`)

## 전달 요구사항
1. `GET /admin/staff/list` 엔드포인트 구현
2. 직원 목록 SSR 렌더링
3. DB 연동 기반 목록 조회
4. 검색 조건
   - 키워드: `이름`, `로그인 아이디`
   - 역할 필터
   - 재직 상태 필터
5. 페이징 지원
6. 재직 상태는 기존 `Staff` 도메인 값 재사용 우선

## 제약 조건
- 관리자 영역 URL 규칙 유지: `/admin/**`
- SSR 방식 유지
- 현재 프로젝트 구조와 DTO 네이밍 규칙 준수
- 가능하면 `HttpServletRequest` 기반 뷰 바인딩 패턴 유지

## 엣지 케이스
- 검색 결과 없음
- 키워드 없음 + 역할/상태만 필터링
- 역할/상태 전체 선택
- 페이지 범위 초과 요청

## 수용 기준
- [ ] `/admin/staff/list` 접속 시 직원 목록이 렌더링된다
- [ ] 이름/로그인 아이디 키워드 검색이 동작한다
- [ ] 역할 필터가 동작한다
- [ ] 재직 상태 필터가 동작한다
- [ ] 페이징이 동작한다
- [ ] DB 조회 기반으로 테스트가 통과한다

## 구현 순서
1. `Staff` 도메인/리포지토리 구조와 기존 상태값 확인
2. 직원 목록 조회용 Repository 쿼리 설계
3. 목록/필터/페이징 DTO 설계
4. `AdminStaffService` 목록 조회 구현
5. `AdminStaffController`의 `GET /admin/staff/list` 구현
6. `staff-list.mustache`에 검색/필터/페이징 렌더링 연결
7. Controller/Service/Repository 테스트 작성 및 검증
