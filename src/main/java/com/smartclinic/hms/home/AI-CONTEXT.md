<!-- Parent: ../AI-CONTEXT.md -->

# home — 홈 메인

## 목적

사이트 진입점. 비회원(환자)과 직원 로그인으로 분기하는 랜딩 페이지.

## 주요 파일

| 파일 | 설명 |
|------|------|
| HomeController.java | `GET /` → `home/index` 렌더링 |

## 동작

```
GET /
→ home/index.mustache (L1 레이아웃, 비회원)
  ├── "환자용" → /reservation
  └── "직원용" → /login
```

## AI 작업 지침

- `GET /` 컨트롤러 파라미터: `HttpServletRequest`
- `request.setAttribute("pageTitle", "홈")`
- L1 레이아웃: `header-public` + `footer-public` 사용

## 의존성

- 내부: 없음 (정적 화면)
- 뷰: `templates/home/index.mustache`
