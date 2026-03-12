<!-- Parent: ../AI-CONTEXT.md -->

# common/interceptor

## 목적

HTTP 요청 처리 전후에 수행될 공통 로직(레이아웃 데이터 설정 등)을 담당한다.

## 주요 파일

| 파일명 | 설명 |
|--------|------|
| LayoutModelInterceptor.java | Mustache 레이아웃 렌더링 시 필요한 공통 모델 데이터(사용자 정보, 메뉴 등)를 `ModelAndView`에 설정한다. |

## AI 작업 지침

- 모든 컨트롤러 요청에 대해 공통적으로 노출해야 할 데이터는 이 인터셉터에서 처리한다.
- `WebMvcConfig`에서 인터셉터 등록 및 제외 경로를 관리한다.

## 의존성

- 내부: `config/WebMvcConfig` (등록용)
