<!-- Parent: ../AI-CONTEXT.md -->

# templates/auth

## 목적

로그인 및 인증 관련 Mustache 템플릿.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| login.mustache | 로그인 페이지 (ID/PW 입력 폼) |

## AI 작업 지침

- 로그인 폼은 `POST /login`으로 데이터를 전송하며, Spring Security의 기본 인증 설정을 따른다.
- 에러 발생 시 쿼리 파라미터 `?error`를 통해 메시지를 표시할 수 있다.
