# Controller Rule (Common)

## 목적
- 모든 기능에서 **SSR 컨트롤러**와 **API 컨트롤러**의 책임을 명확히 분리한다.
- 컨트롤러는 입출력/라우팅만 담당하고, 비즈니스 로직은 Service 계층으로 위임한다.

## 1) Controller 종류와 역할

### 1. SSR Controller (`...Controller`)
- 어노테이션: `@Controller`
- 역할: 템플릿 렌더링
- 반환값: `String` (뷰 경로)
- 모델 전달: `Model` 또는 `HttpServletRequest#setAttribute(...)`
- 예시: `GET /{role}/{feature}` -> `"{role}/{feature}"`

### 2. API Controller (`...ApiController`)
- 어노테이션: `@RestController`
- 역할: JSON 응답
- 반환값: `ResponseEntity<?>`
- 응답 포맷: `Resp.ok(data)` / 공통 예외 포맷
- 예시: `GET /{role}/{feature}/**`

## 2) 구현 규칙

1. 컨트롤러는 Service만 호출한다.
- 금지: 컨트롤러에서 Repository/JPA 직접 호출

2. SSR과 API URL 책임을 분리한다.
- SSR: 화면 렌더링 경로
- API: 데이터 조회/처리 경로

3. 요청/응답 DTO를 명확히 분리한다.
- 입력: `...Request`
- 출력: `...Response`

4. API 응답 형식을 통일한다.
- 성공: `Resp.ok(...)`
- 실패: `GlobalExceptionHandler` 표준 포맷 사용

5. 권한 정책은 SecurityConfig를 따른다.
- 컨트롤러에서 권한 완화/우회 로직 금지

6. 컨트롤러 메서드는 얇게 유지한다.
- 파라미터 바인딩 -> 서비스 호출 -> 반환

## 3) 코드 스켈레톤

### SSR
```java
@Controller
@RequiredArgsConstructor
@RequestMapping("/{role}")
public class XxxController {

    private final XxxService service;

    @GetMapping("/{feature}")
    public String page(HttpServletRequest req) {
        var model = service.getPageModel();
        req.setAttribute("model", model);
        return "{role}/{feature}";
    }
}
```

### API
```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/{role}/{feature}")
public class XxxApiController {

    private final XxxService service;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        var data = service.getStats();
        return Resp.ok(data);
    }
}
```

## 4) 네이밍 규칙
- SSR 컨트롤러: `...Controller`
- API 컨트롤러: `...ApiController`
- 서비스: `...Service`
- 요청 DTO: `...Request`
- 응답 DTO: `...Response`

## 5) 체크리스트
- [ ] SSR 컨트롤러가 뷰 경로(`String`)를 반환하는가
- [ ] API 컨트롤러가 `ResponseEntity<?>` + `Resp.ok(...)`를 사용하는가
- [ ] 컨트롤러가 Service 외 계층을 직접 호출하지 않는가
- [ ] 입력/출력 DTO가 분리되어 있는가
- [ ] URL/권한 규칙이 기존 prefix 정책을 유지하는가
- [ ] 비즈니스 로직이 컨트롤러에 들어가지 않았는가
