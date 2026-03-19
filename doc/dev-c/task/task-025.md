# Task 025 - 규칙 목록 완성

## Task 25-1. 현재 규칙 목록 구조 점검
- [x] `AdminRuleController`, `AdminRuleService`, Repository, 템플릿 현재 구조 확인
- [x] 기존 목록 조회 계약과 페이지네이션 유무 확인
- [x] 필터/검색/상태 배지 추가 시 영향 범위 메모 정리

## Task 25-2. 목록 응답 구조와 조회 책임 설계
- [x] 규칙 목록 응답 DTO 설계
- [x] 페이지 링크 DTO 설계
- [x] 카테고리/활성/키워드/페이지 파라미터 기본값 정리
- [x] Repository 조회 책임과 정렬 기준 확정

## Task 25-3. 규칙 목록 검색 + 필터 + 페이지네이션 구현
- [ ] 카테고리 필터 구현
- [ ] 활성 여부 필터 구현
- [ ] 제목 키워드 검색 구현
- [ ] 기본 `page=1`, `size=10` 페이지네이션 구현
- [ ] 최신 등록순 정렬 적용

## Task 25-4. 규칙 목록 Mustache 화면 구현
- [ ] 검색/필터 바 추가
- [ ] 활성 상태 배지 표시 추가
- [ ] 결과 없을 때 빈 목록 메시지 추가
- [ ] 필터/검색 조건 유지형 페이지네이션 UI 반영

## Task 25-5. 테스트 보강
- [ ] 컨트롤러 테스트에 파라미터 전달과 렌더링 검증 추가
- [ ] 서비스 테스트에 필터/검색/정렬/빈 결과 검증 추가
- [ ] 필요 시 Repository 테스트 또는 쿼리 검증 추가

## Task 25-6. 문서 및 최종 검증 마무리
- [ ] `workflow-025` 완료 처리
- [ ] `task-025` 완료 처리
- [ ] `admin.rule` 또는 관련 범위 테스트 실행
- [ ] 필요 시 전체 `./gradlew test` 재확인

## Task 25-1 점검 메모
- `AdminRuleController`
  - 현재 `GET /admin/rule/list`는 파라미터를 전혀 받지 않고 `Model`에 `rules`, `hasRules`, `pageTitle`만 넣는다.
  - `page`, `size`, `category`, `active`, `keyword` 전달 구조가 아직 없다.
- `AdminRuleService`
  - 현재 `getRuleList()`는 전체 조회 `findAllByOrderByCreatedAtDesc()`만 수행한다.
  - 필터/검색/페이지네이션 로직이 전혀 없다.
- `HospitalRuleRepository`
  - 현재 메서드는 `findAllByOrderByCreatedAtDesc()` 하나뿐이다.
  - 카테고리, 활성 여부, 제목 검색, 페이지 조회를 위한 메서드 또는 `@Query`가 필요하다.
- `AdminRuleDto`
  - 현재 카테고리 표시 텍스트, 활성 여부 텍스트, 활성 상태 배지 클래스는 이미 가지고 있다.
  - 활성 상태 표시 자체는 재사용 가능하지만 목록 응답 DTO와 페이지 링크 DTO는 별도 추가가 필요하다.
- `rule-list.mustache`
  - 현재는 카드형 목록만 있고 검색 바, 필터 UI, 페이지네이션 UI가 없다.
  - 한글 인코딩 손상도 있어 템플릿 전체 정리가 필요하다.
- 테스트 상태
  - `AdminRuleControllerTest`, `AdminRuleServiceTest`는 현재 전체 조회 기준만 검증한다.
  - 검색/필터/페이지네이션 관련 테스트가 없다.
- 도메인 메모
  - `HospitalRule`은 `category`, `active`, `createdAt`을 이미 가지고 있어 이번 목록 완성에 필요한 도메인 필드는 충분하다.
  - 정렬 기준은 `createdAt desc`를 그대로 유지하면 자연스럽다.

## Task 25-2 설계 메모
- 목록 row 데이터는 기존 `AdminRuleDto`를 그대로 재사용한다.
- 바깥쪽 응답은 `AdminRuleListResponse`로 감싼다.
  - `rules`
  - `pageLinks`
  - `selectedCategory`
  - `selectedActive`
  - `keyword`
  - `totalCount`
  - `currentPage`, `size`, `totalPages`
  - `hasPages`, `hasPrevious`, `hasNext`
  - `previousUrl`, `nextUrl`
- 페이지 링크는 `AdminRulePageLinkResponse(page, url, active)` 구조로 통일한다.
- 파라미터 기본값은 아래처럼 고정한다.
  - `page=1`
  - `size=10`
  - `category=ALL`
  - `active=ALL`
  - `keyword=''`
- 조회 책임은 아래처럼 나눈다.
  - Controller: 요청 파라미터 수신, `model + pageTitle` 렌더링
  - Service: 파라미터 정규화, 페이지 링크 생성, 응답 DTO 조합
  - Repository: 카테고리/활성/제목 검색 + 최신순 조회
- 정렬 기준은 `createdAt desc, id desc`를 기본으로 잡는다.
- 연락처나 본문 검색은 이번 범위에 넣지 않고 제목 검색만 지원한다.

## 리뷰 포인트
- 규칙 목록을 대시보드가 아닌 관리용 검색형 인덱스 화면으로 두는 구성이 적절한지
- 카테고리/활성/키워드 필터 조합 시 현재 조건을 유지하는 URL 설계가 자연스러운지
- 활성 상태 배지와 필터 UI가 기존 관리자 목록 패턴과 충분히 일관적인지