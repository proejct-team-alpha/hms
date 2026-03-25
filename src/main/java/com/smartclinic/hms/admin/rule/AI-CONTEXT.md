<!-- Parent: ../AI-CONTEXT.md -->

# admin/rule

## 목적

병원 운영 규칙(HospitalRule) CRUD와 관리자 전용 삭제 JSON API 기능을 제공한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminRuleController.java | 병원 규칙 목록, 등록, 상세, 수정 컨트롤러 (`/admin/rule`) |
| AdminRuleApiController.java | 규칙 삭제 관리자 JSON API 컨트롤러 (`POST /admin/api/rules/{id}`) |
| AdminRuleService.java | 병원 규칙 목록/등록/상세/수정/삭제 서비스 |
| HospitalRuleRepository.java | `HospitalRule` 조회 및 검색 저장소 |
| dto/CreateAdminRuleRequest.java | 규칙 등록 요청 DTO |
| dto/UpdateAdminRuleRequest.java | 규칙 수정 요청 DTO |
| dto/AdminRuleDetailResponse.java | 규칙 상세 화면 응답 DTO |
| dto/AdminRuleDeleteResponse.java | 규칙 삭제 성공 응답 DTO |

## AI 작업 지침

- 병원 규칙은 카테고리별로 관리될 수 있다.
- 수정 화면은 `GET /admin/rule/detail?ruleId={id}` 상세 화면 안에서 폼을 함께 보여주는 SSR 구조를 사용한다.
- 등록/수정 폼은 `admin/_rule-form.mustache` partial 기준으로 재사용한다.
- 삭제는 `POST /admin/api/rules/{id}` 관리자 JSON API로 처리하며, 성공 시 `Resp.ok(...)` 형식의 응답을 반환한다.
- 규칙 삭제는 비활성화가 아니라 물리 삭제이며, 후속 UX는 클라이언트가 결정한다.

## 의존성

- 내부: `domain/HospitalRule`
