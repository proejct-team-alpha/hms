<!-- Parent: ../AI-CONTEXT.md -->

# admin/rule

## 목적

병원 운영 규칙(HospitalRule) 및 안내 문구 CRUD 기능을 제공한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminRuleController.java | 병원 규칙 목록, 등록, 상세, 수정 컨트롤러 (`/admin/rule`) |
| AdminRuleService.java | 병원 규칙 목록/등록/상세/수정 서비스 |
| HospitalRuleRepository.java | `HospitalRule` 조회 및 검색 저장소 |
| dto/CreateAdminRuleRequest.java | 규칙 등록 요청 DTO |
| dto/UpdateAdminRuleRequest.java | 규칙 수정 요청 DTO |
| dto/AdminRuleDetailResponse.java | 규칙 상세 화면 응답 DTO |

## AI 작업 지침

- 병원 규칙은 카테고리별로 관리될 수 있다.
- 수정 화면은 `GET /admin/rule/detail?ruleId={id}` 상세 화면 안에서 폼을 함께 보여주는 SSR 구조를 사용한다.
- 등록/수정 폼은 `admin/_rule-form.mustache` partial 기준으로 재사용한다.

## 의존성

- 내부: `domain/HospitalRule`
