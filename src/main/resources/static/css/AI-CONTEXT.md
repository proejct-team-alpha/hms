<!-- Parent: ../AI-CONTEXT.md -->

# static/css

## 목적

애플리케이션 전반에서 사용하는 스타일시트 파일들.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| common.css | 전역 공통 스타일 |
| admin-style.css | 관리자 페이지 전용 스타일 |
| doctor-style.css | 의사 페이지 전용 스타일 |
| home-style.css | 메인 홈페이지 스타일 |
| style-staff.css | 스태프 페이지 전용 스타일 |
| input.css | Tailwind CSS 소스 파일 (빌드용) |

## AI 작업 지침

- 스타일 수정 시 가급적 BEM 명명 규칙을 따르거나 Tailwind CSS 유틸리티 클래스를 활용한다.
- `input.css` 수정 시 빌드 프로세스를 통해 `style.css`가 업데이트되는지 확인한다.
