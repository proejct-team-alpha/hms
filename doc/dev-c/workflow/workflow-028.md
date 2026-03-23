# 규칙 삭제 JSON API 구현 명세서 (workflow-028)

## 문제 정의
관리자 전용 규칙 삭제 API를 구현한다. 이 API는 SSR 리다이렉트용이 아니라 AJAX 호출용 JSON API이며, `POST /admin/api/rules/{id}`로 호출해 병원 규칙을 실제로 삭제해야 한다. 성공 시 서버는 공통 응답 래퍼 `Resp.ok(...)` 형식의 JSON만 반환하고, 이후 목록 갱신/행 제거/페이지 이동/toast 같은 후속 UX는 클라이언트가 결정한다.

## 대상 URL
- `POST /admin/api/rules/{id}`

## 비채택 경로
- `DELETE /api/rules/{id}`
- `DELETE /admin/api/rules/{id}`

## 구현 범위
1. 관리자 전용 규칙 삭제 API `POST /admin/api/rules/{id}` 구현
2. 요청 바디 없이 `path variable id`만으로 삭제 처리
3. 규칙을 비활성화가 아닌 실제 엔티티 물리 삭제로 처리
4. 성공 시 `Resp.ok(...)` 기반 JSON 응답 반환
5. 삭제 대상이 없으면 `CustomException.notFound(...)` 기반 공통 JSON 실패 응답 처리
6. 관련 API 컨트롤러/서비스 테스트 추가 및 검증

## 응답 필드
- 성공 응답 래퍼
  - `status`
  - `msg`
  - `body`
- 성공 body
  - `ruleId`
  - `message`

## 성공 응답 예시
```json
{
  "status": 200,
  "msg": "성공",
  "body": {
    "ruleId": 15,
    "message": "규칙이 삭제되었습니다."
  }
}
```

## 실패 처리
- 존재하지 않는 `id` 삭제 요청 시 `CustomException.notFound(...)`를 던진다.
- 실패 응답은 `GlobalExceptionHandler -> Resp.fail(...)` 흐름을 그대로 사용한다.
- 삭제 API에서 별도의 수동 실패 JSON 바디는 만들지 않는다.
- 잘못된 `id` 형식 등 path variable 바인딩 실패는 공통 검증 실패 응답 규칙을 따른다.

## 후속 UX 처리 원칙
- 서버는 삭제 성공 JSON만 반환한다.
- 후속 UX는 클라이언트가 결정한다.
  - 목록 새로고침
  - 삭제된 행 제거
  - 목록 페이지 이동
  - 성공 toast 표시

## 제약 조건
- 관리자 전용 API이므로 `/admin/**` 보안 규칙을 따른다.
- JSON 응답 API는 기존 공통 래퍼 [Resp.java](/c:/Users/HSystem/hms/src/main/java/com/smartclinic/hms/common/util/Resp.java) 패턴을 유지한다.
- 성공 응답은 `ResponseEntity<Resp<...>>` + `Resp.ok(...)` 조합을 사용한다.
- 삭제는 `active=false` 전환이 아닌 `Repository.delete(...)` 또는 동등한 물리 삭제 방식이어야 한다.
- 요청 바디 없이 경로 변수만 받는 단순 삭제 API로 구현한다.
- 성공 메시지는 기본적으로 `규칙이 삭제되었습니다.`를 사용한다.

## 엣지 케이스
- 존재하지 않는 `id` 삭제 요청
- 이미 삭제된 대상을 다시 삭제하려는 요청
- 권한 없는 사용자의 호출
- 잘못된 `id` 형식 요청
- 클라이언트가 성공 응답은 받았지만 후속 UX 처리에 실패하는 경우

## 수용 기준
- [x] `POST /admin/api/rules/{id}`가 관리자 권한에서 동작한다.
- [x] 요청 바디 없이 `id`만 받아 규칙을 실제 삭제한다.
- [x] 성공 시 `Resp.ok(...)` 형식의 JSON 응답을 반환한다.
- [x] 성공 body에는 `ruleId`와 `message`가 포함된다.
- [x] 삭제 대상이 없으면 `CustomException.notFound(...)` 기반 JSON 실패 응답이 반환된다.
- [x] 관련 API 컨트롤러/서비스 테스트가 통과한다.

## 구현 순서
1. 현재 `admin.rule` 모듈의 컨트롤러/서비스/리포지토리 구조 확인
2. 삭제 성공 응답 DTO 설계
3. `POST /admin/api/rules/{id}` API 컨트롤러 추가
4. 서비스에 규칙 삭제 메서드 추가
5. not found 및 공통 실패 응답 흐름 연결
6. 관리자 권한/보안 테스트 확인
7. API 컨트롤러/서비스 테스트 작성 및 검증
8. 문서/리포트 정리

## 리뷰 사인오프
- `Resp.ok(...)` 래퍼와 body DTO의 역할이 명확히 분리되어 있는지
- 삭제를 soft delete가 아닌 물리 삭제로 처리하는 점이 문서/코드에서 일관적인지
- SSR 화면 리다이렉트 로직이 섞이지 않고 JSON API 책임만 유지하는지
- 향후 클라이언트 UX 확장 시 서버 계약을 다시 바꿀 필요 없이 사용할 수 있는지

## 후속 TODO
- 구현 후 로컬 `API.md` 또는 관련 문서에 남아 있는 규칙 삭제 계약이 실제 코드 기준과 다르면 정리한다.
- 관리자 규칙 상세 화면이나 목록 화면에서 이 JSON 삭제 API를 호출하는 프런트 흐름은 별도 task로 분리해 정리할 수 있다.
