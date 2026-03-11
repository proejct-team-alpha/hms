# HMS Controller 규칙

> **기준**: Spring Boot 4.0.x + Mustache SSR + JSON API

본 문서는 Controller 계층의 책임 분리와 응답 형식 일관성을 정의한다.
핵심은 **SSR Controller**와 **API Controller**를 분리하고, 비즈니스 로직을 Service 계층으로 위임하는 것이다.

---

## 1. 목적

- 화면 렌더링(SSR)과 데이터 응답(JSON API)의 책임을 분리한다.
- Controller는 라우팅/입출력 바인딩에 집중하고, 도메인 로직은 Service가 담당한다.
- API 응답 형식을 `Resp.ok(...)`로 통일해 클라이언트 처리 일관성을 확보한다.

---

## 2. Controller 분리 원칙

### 2.1 SSR Controller

- 어노테이션: `@Controller`
- 반환 타입: `String` (뷰 경로)
- 주요 역할:
  - 요청 파라미터 수신
  - Service 호출
  - Model/Request attribute 세팅
  - Mustache 템플릿 반환

예시:
- `GET /admin/dashboard` -> `"admin/dashboard"`

### 2.2 API Controller

- 어노테이션: `@RestController`
- 반환 타입: `ResponseEntity<?>`
- 주요 역할:
  - 요청 파라미터/DTO 수신
  - Service 호출
  - JSON 응답 반환

예시:
- `GET /admin/dashboard/stats` -> `Resp.ok(data)`

---

## 3. 구현 규칙

1. Controller는 Service만 호출한다.
- 금지: Controller에서 Repository/JPA 직접 호출

2. SSR과 API URL을 분리한다.
- SSR: 템플릿 렌더링 엔드포인트
- API: 데이터 조회/처리 엔드포인트
- URL을 새로 만들거나 변경할 때는 외부 문서 저장소 `https://github.com/proejct-team-alpha/documents`를 참고해 사전 정의된 주소만 사용한다.

3. DTO 네이밍을 고정한다.
- 요청 DTO: `...Request`
- 응답 DTO: `...Response`

4. API 성공 응답은 `Resp.ok(...)`를 사용한다.
- 표준 포맷을 벗어난 임의 JSON 직접 조립을 지양한다.

5. 예외 응답은 공통 처리기를 따른다.
- `GlobalExceptionHandler` 포맷 유지

6. 권한 정책은 SecurityConfig 기준을 따른다.
- Controller 내부에서 권한 우회/완화 금지

7. Controller 메서드는 얇게 유지한다.
- 바인딩 -> 서비스 호출 -> 반환 흐름만 유지

---

## 4. DTO 네이밍 규칙

- 요청: `CreateXxxRequest`, `UpdateXxxRequest`, `SearchXxxRequest`
- 응답: `XxxResponse`, `XxxDetailResponse`, `XxxListResponse`
- 통계/집계: `XxxStatsResponse`

---

## 5. 체크리스트

- [ ] SSR Controller가 뷰 경로(`String`)를 반환하는가
- [ ] API Controller가 `ResponseEntity<?>` + `Resp.ok(...)`를 사용하는가
- [ ] Controller가 Service 외 계층을 직접 호출하지 않는가
- [ ] Request/Response DTO 네이밍이 규칙을 따르는가
- [ ] URL/권한 규칙이 기존 prefix 정책을 유지하는가
- [ ] URL 생성/변경 시 외부 문서 저장소 기준의 사전 정의 주소만 사용했는가
- [ ] 비즈니스 로직이 Controller에 들어가지 않았는가

---

## 6. 요약

- **SSR은 렌더링, API는 JSON**으로 분리한다.
- **Controller는 얇게, Service는 두껍게** 유지한다.
- **응답 형식은 `Resp.ok` + 공통 예외 포맷**으로 통일한다.
