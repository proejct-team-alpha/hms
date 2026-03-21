# admin.rule 패키지 — 구성 파일 목록

> ■ 비전공자 C 소유

| 파일 | 설명 |
|------|------|
| AdminRuleController.java | CRUD: GET/POST /admin/rule/** |
| AdminRuleApiController.java | 삭제 API: POST /admin/api/rules/{id} |
| AdminRuleService.java | 병원 규칙 CRUD 서비스 |
| HospitalRuleRepository.java | HospitalRule CRUD |
| dto/CreateAdminRuleRequest.java | 규칙 등록 요청 DTO |
| dto/UpdateAdminRuleRequest.java | 규칙 수정 요청 DTO |
| dto/AdminRuleDetailResponse.java | 규칙 상세 응답 DTO |
| dto/AdminRuleDeleteResponse.java | 규칙 삭제 응답 DTO |
