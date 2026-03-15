<!-- Parent: ../AI-CONTEXT.md -->

# admin/rule

## 목적

병원 운영 규칙(HospitalRule) 및 안내 문구 CRUD 기능을 제공한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| AdminRuleController.java | 병원 규칙 등록, 목록, 수정 컨트롤러 (`/admin/rule`) |
| (HospitalRuleRepository.java) | 실제 위치 확인 필요 (보통 admin 패키지 내부에 있음) |

## AI 작업 지침

- 병원 규칙은 카테고리별로 관리될 수 있다.

## 의존성

- 내부: `domain/HospitalRule`
