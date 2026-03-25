# 규칙 수정 상세 화면 연동 및 저장 명세서 (workflow-027)

## 문제 정의
관리자 규칙 수정 기능 `S32`를 완성한다. 규칙 상세 화면 `GET /admin/rule/detail?ruleId={id}`에서 기존 규칙 정보를 조회하고, 같은 화면 안에서 제목, 내용, 카테고리, 활성 여부를 수정할 수 있어야 한다. 수정 저장은 `POST /admin/rule/update`로 처리하고, 성공 시 다시 상세 화면으로 돌아와 성공 메시지를 보여줘야 한다.

## 대상 URL
- `GET /admin/rule/detail?ruleId={id}`
- `POST /admin/rule/update`

## 비채택 경로
- `GET /admin/rule/edit/{id}`
- `POST /admin/rule/edit/{id}`

## 구현 범위
1. 규칙 상세 조회 화면 `GET /admin/rule/detail?ruleId={id}` 구현
2. 상세 화면 안에 수정 폼을 함께 제공
3. 수정 저장 처리를 `POST /admin/rule/update`로 구현
4. 제목, 내용, 카테고리, 활성 여부 전체 필드 수정 지원
5. 저장 성공 시 상세 화면 리다이렉트 + 성공 메시지 표시
6. 저장 실패 시 같은 상세 화면 재렌더링 + 입력값 유지 + 오류 메시지 표시
7. 등록 화면과 수정 화면의 폼 구조를 최대한 재사용

## 입력 필드
- `ruleId`
  - 필수
  - 수정 대상 규칙 식별자
- `title`
  - 필수
  - 최대 200자
- `content`
  - 필수
  - 최대 3000자
- `category`
  - 필수
  - `EMERGENCY`, `SUPPLY`, `DUTY`, `HYGIENE`, `OTHER`
- `isActive`
  - 체크박스 입력
  - 체크 시 `true`
  - 체크 해제 시 `false`

## 저장 이후 흐름
- 저장 성공 시 `redirect:/admin/rule/detail?ruleId={id}`
- 상세 화면에서 성공 플래시 메시지 표시
- 성공 메시지 문구는 기본적으로 `규칙이 수정되었습니다.`를 사용한다.

## 검증/실패 처리
- 제목 미입력 시 오류 메시지 표시
- 제목 200자 초과 시 오류 메시지 표시
- 내용 미입력 시 오류 메시지 표시
- 내용 3000자 초과 시 오류 메시지 표시
- 카테고리 미선택 시 오류 메시지 표시
- 잘못된 카테고리 문자열 전송 시 친절한 오류 메시지 표시
- 검증 실패 시 `GET /admin/rule/detail?ruleId={id}`와 같은 상세 화면 구조를 다시 렌더링한다.
- 검증 실패 시 사용자가 입력한 제목, 내용, 카테고리, 활성 여부를 유지한다.

## 기대 화면 성격
- 관리자 전용 SSR 상세/수정 화면
- 상세 정보와 수정 폼이 한 화면에 공존하는 구조
- 등록 화면과 동일한 관리자 레이아웃 패턴 유지
- 수정 폼은 신규 화면을 별도 설계하기보다 기존 등록 폼 자산을 최대한 재사용
- 가능하면 `admin/_rule-form.mustache` 같은 공통 폼 partial을 두고 `rule-new.mustache`, `rule-detail.mustache`가 이를 공유

## 제약 조건
- 관리자 영역 URL prefix(`/admin/**`)를 유지한다.
- 수정 기능의 조회 기준 URL은 `GET /admin/rule/detail?ruleId={id}`로 고정한다.
- 수정 저장 URL은 `POST /admin/rule/update`로 고정한다.
- 별도의 edit 전용 화면은 만들지 않고 상세 화면 내부 수정 폼 방식으로 구현한다.
- 등록/수정 폼의 차이는 제목, action URL, 버튼 문구, 초기값 바인딩 정도로 제한한다.
- `HospitalRule` 엔티티의 `title` 길이 제한 200자를 따른다.
- 등록 기능에서 이미 정리한 `category` enum, `active` 체크박스 처리 규칙과 일관성을 유지한다.

## 엣지 케이스
- 존재하지 않는 `ruleId`로 상세 조회 시 not found 처리
- 존재하지 않는 `ruleId`로 수정 저장 시 not found 처리
- 활성 체크박스를 해제한 경우 `false`로 저장
- 검증 실패 후에도 체크박스 상태가 유지되어야 한다.
- 잘못된 카테고리 문자열 입력 시 상세 화면 same-view 재렌더링이 되어야 한다.
- 상세 조회 모델과 수정 폼 모델이 한 화면에서 충돌하지 않아야 한다.

## 수용 기준
- [x] `GET /admin/rule/detail?ruleId={id}`에서 규칙 상세와 수정 폼이 함께 렌더링된다.
- [x] 제목, 내용, 카테고리, 활성 여부를 수정할 수 있다.
- [x] 저장 성공 시 같은 상세 화면으로 리다이렉트된다.
- [x] 저장 성공 후 상세 화면에 성공 메시지가 표시된다.
- [x] 저장 실패 시 상세 화면이 재렌더링되고 입력값이 유지된다.
- [x] 저장 실패 시 필드 오류 메시지가 표시된다.
- [x] 등록 화면과 수정 화면의 폼 구조가 공통 패턴으로 재사용된다.
- [x] 관련 컨트롤러/서비스/뷰 테스트가 통과한다.

## 구현 순서
1. 현재 `AdminRuleController`, `AdminRuleService`, 규칙 상세/등록 템플릿 구조 점검
2. 수정 요청 DTO와 검증 규칙 설계
3. `GET /admin/rule/detail?ruleId={id}` 상세 조회 + 수정 폼 모델 구성
4. `POST /admin/rule/update` 저장 구현
5. 검증 실패 재렌더링 및 입력값 유지 처리
6. 성공 플래시 메시지 및 상세 화면 리다이렉트 처리
7. 등록/수정 폼 공통 partial 또는 재사용 구조 정리
8. 컨트롤러/서비스 테스트 및 문서 정리

## 리뷰 포인트
- 상세 조회 모델과 수정 폼 모델을 한 화면에서 어떻게 충돌 없이 유지할지
- `rule-new`와 `rule-detail` 사이 폼 partial 추출 범위를 어디까지 가져갈지
- 기존 로컬 문서에 남아 있을 수 있는 `/admin/rule/edit/{id}` 또는 `/admin/rule/update` 계약 차이를 어떻게 정리할지

## 후속 TODO
- 구현 후 로컬 `API.md`와 관련 문서의 규칙 수정 URL 계약을 실제 코드 기준으로 다시 점검한다.
