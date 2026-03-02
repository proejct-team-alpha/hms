# Spring Boot 플랫폼 가이드 (Back-end)

> **기준 버전**: Spring Boot 4.0.x · Java 21 · Spring Security 7.0.x

백엔드에서는 **보안 규정**과 **데이터 무결성**에 집중한다.

---

## 목차

1. [환경 변수 관리](#1-보안-환경-변수-관리)
2. [전역 에러 핸들러](#2-기능-전역-에러-핸들러-global-exception-handler)
3. [URI 설계 및 계층형 자원](#3-api-uri-설계-및-계층형-자원-구현)
4. [API 버저닝](#4-api-api-버저닝)
5. [서비스 레이어 구조](#5-기능-서비스-레이어-구조)
6. [Java Record 사용 (DTO)](#6-기술-java-record-사용-dto)
7. [Validation](#7-기능-validation)
8. [페이징 처리](#8-기능-페이징-처리)
9. [HTTP 상태 코드 가이드](#9-api-http-상태-코드-가이드)
10. [Spring Security — 인증·인가·CORS](#10-보안-spring-security-인증-인가-cors)

---

<a id="1-보안-환경-변수-관리"></a>

## 1. [보안] 환경 변수 관리

**Rule**: DB 비밀번호 등 민감 정보는 `application.yml`에 직접 노출하지 않고 **환경 변수**를 사용한다.

```yaml
# Good Case
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

- `.env` 파일, `run-dev.ps1` 등 시작 스크립트에서 주입
- `application-prod.yml` 에서도 하드코딩 금지
- 시크릿 파일(`.env`, `run-dev.ps1`)은 `.gitignore`에 반드시 추가

---

<a id="2-기능-전역-에러-핸들러-global-exception-handler"></a>

## 2. [기능] 전역 에러 핸들러 (Global Exception Handler)

**Rule**: 모든 예외는 **공통된 `ErrorResponse` 형식**으로 반환한다. Validation 예외·도메인 예외·예측 불가 예외를 모두 핸들러에서 처리한다.

### ErrorResponse 구조

운영 환경에서 문제 추적에 필요한 `timestamp`, `path`, `traceId` 를 포함한다.

```java
/**
 * 공통 에러 응답.
 * traceId — MDC(Micrometer Tracing) 또는 Sleuth에서 자동 주입.
 * details — validation 오류 필드 목록 등 부가 정보.
 */
public record ErrorResponse(
    String         code,
    String         message,
    Instant        timestamp,
    String         path,
    String         traceId,
    Map<String, Object> details
) {
    /** 단순 오류 (details 없음) */
    public static ErrorResponse of(String code, String message,
                                   HttpServletRequest req) {
        return new ErrorResponse(
            code, message,
            Instant.now(),
            req.getRequestURI(),
            MDC.get("traceId"),   // Micrometer Tracing 자동 설정 시 자동 채워짐
            Map.of()
        );
    }

    /** Validation 오류 (fields 포함) */
    public static ErrorResponse ofValidation(String message,
                                             Map<String, Object> details,
                                             HttpServletRequest req) {
        return new ErrorResponse(
            "VALIDATION_ERROR", message,
            Instant.now(),
            req.getRequestURI(),
            MDC.get("traceId"),
            details
        );
    }
}
```

### GlobalExceptionHandler

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 1. Bean Validation (@Valid — 요청 바디) ───────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, Object> details = ex.getBindingResult()
            .getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "invalid",
                (a, b) -> a   // 동일 필드 중복 시 첫 번째 메시지 유지
            ));

        String message = details.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
            .body(ErrorResponse.ofValidation(message, details, req));
    }

    // ── 2. Bean Validation (@Validated — Path/Query 파라미터) ─────────────
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {

        Map<String, Object> details = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                v -> v.getPropertyPath().toString(),
                v -> v.getMessage(),
                (a, b) -> a
            ));

        return ResponseEntity.badRequest()
            .body(ErrorResponse.ofValidation(ex.getMessage(), details, req));
    }

    // ── 3. 도메인 비즈니스 예외 ──────────────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest req) {

        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage(), req));
    }

    // ── 4. 리소스 없음 ────────────────────────────────────────────────────
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            EntityNotFoundException ex, HttpServletRequest req) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of("RESOURCE_NOT_FOUND", ex.getMessage(), req));
    }

    // ── 5. 예측 불가 예외 (폴백) ─────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest req) {

        log.error("[GlobalExceptionHandler] 예측 불가 예외: path={}", req.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
            .body(ErrorResponse.of("INTERNAL_ERROR", "서버 오류가 발생했습니다.", req));
    }
}
```

### BusinessException 설계

```java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = httpStatus;
    }

    // 편의 팩토리 메서드
    public static BusinessException notFound(String message) {
        return new BusinessException("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
    public static BusinessException conflict(String message) {
        return new BusinessException("ALREADY_EXISTS", message, HttpStatus.CONFLICT);
    }
    public static BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }
}
```

> **Spring Boot 4.0.x 참고**: `spring.mvc.problemdetails.enabled=true` 설정 시
> RFC 9457 Problem Details 포맷을 기본 지원한다. 팀 표준에 따라 채택 여부를 결정한다.

---

<a id="3-api-uri-설계-및-계층형-자원-구현"></a>

## 3. [API] URI 설계 및 계층형 자원 구현

**Rule**: 계층형 자원 지향 URI를 적용하고, `@RequestMapping`·`@PathVariable`로 계층을 표현한다.

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // 1. 컬렉션
    @GetMapping
    public Page<OrderResponse> getOrders(@ModelAttribute OrderSearchCond cond,
                                         @PageableDefault(size = 20) Pageable pageable) {
        return orderService.search(cond, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@Valid @RequestBody OrderCreateRequest req) {
        return orderService.create(req);
    }

    // 2. 단일 리소스
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable Long orderId) {
        return orderService.getDetail(orderId);
    }

    @PatchMapping("/{orderId}")
    public OrderResponse updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderUpdateRequest req) {
        return orderService.update(orderId, req);
    }

    // 3. 하위 리소스
    @GetMapping("/{orderId}/items")
    public List<OrderItemResponse> getOrderItems(@PathVariable Long orderId) {
        return orderService.getItems(orderId);
    }

    @PostMapping("/{orderId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse addOrderItem(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemCreateRequest req) {
        return orderService.addItem(orderId, req);
    }

    // 4. 상태 변경 액션 — POST /cancel 방식 허용
    //    일부 팀은 PATCH /{orderId}?status=CANCELLED 로 대체하기도 함 (팀 표준 우선)
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId) {
        return orderService.cancel(orderId);
    }
}
```

**표준 URI 구조 예시** (2026년 기준):

```text
/api/v{version}                     ← v1 생략 가능 (버저닝 전략은 [4](#4-api-api-버저닝) 참조)
  /orders                           ← 컬렉션
  /orders/{orderId}
  /orders/{orderId}/items
  /orders/{orderId}/items/{itemId}
  /orders/{orderId}/cancel          ← 상태 변경 액션 (POST)
  /orders/search                    ← 검색 전용 (GET + query string 권장)
  /me/orders                        ← 현재 사용자 관련
```

---

<a id="4-api-api-버저닝"></a>

## 4. [API] API 버저닝

**Rule**: v1은 URI에서 생략 가능하다. 파괴적 변경(breaking change) 발생 시 반드시 버전을 올린다. 버저닝 전략은 아래 중 하나를 팀 표준으로 통일한다.

### 전략 비교

| 전략                       | 예시                                  | 장점              | 단점                    |
| -------------------------- | ------------------------------------- | ----------------- | ----------------------- |
| **URI 버저닝** (기본 권장) | `/api/v2/orders`                      | 직관적, 캐시 용이 | URL 증가                |
| **Accept 헤더**            | `Accept: application/vnd.app.v2+json` | URI 오염 없음     | 테스트·디버깅 불편      |
| **커스텀 헤더**            | `X-API-Version: 2`                    | URI 오염 없음     | 비표준, 클라이언트 부담 |

### URI 버저닝 구현 예시

```java
// v1 — 기존 유지
@RestController
@RequestMapping("/api/v1/orders")
public class OrderV1Controller { ... }

// v2 — 파괴적 변경 발생 시
@RestController
@RequestMapping("/api/v2/orders")
public class OrderV2Controller { ... }
```

### Accept 헤더 버저닝 구현 예시

```java
// produces 로 버전 분기
@GetMapping(value = "/orders", produces = "application/vnd.app.v2+json")
public OrderV2Response getOrdersV2(...) { ... }

@GetMapping(value = "/orders", produces = "application/vnd.app.v1+json")
public OrderV1Response getOrdersV1(...) { ... }
```

> v1 생략 규칙: 초기 단계에서 `/api/orders`를 사용하고, 파괴적 변경 시 `/api/v2/orders`로 올린다.
> 이 경우 `/api/orders`는 `/api/v1/orders`와 동일하게 유지한다.

---

<a id="5-기능-서비스-레이어-구조"></a>

## 5. [기능] 서비스 레이어 구조

**Rule**: 비즈니스 로직은 `@Service` 레이어에 집중하고, Controller는 입력/출력 변환만 담당한다.

| 레이어       | 역할                   | 금지 사항                |
| ------------ | ---------------------- | ------------------------ |
| `Controller` | 요청 수신, 응답 직렬화 | 비즈니스 로직 작성 금지  |
| `Service`    | 트랜잭션, 도메인 로직  | 직접 HTTP 객체 사용 금지 |
| `Repository` | DB 접근, 쿼리          | 비즈니스 로직 작성 금지  |

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)   // 기본값: 읽기 전용 (성능 최적화)
public class OrderService {

    private final OrderRepository orderRepository;

    // 쓰기 메서드는 개별적으로 @Transactional 선언
    @Transactional
    public OrderResponse create(OrderCreateRequest req) {
        Order order = Order.create(req);
        return OrderResponse.from(orderRepository.save(order));
    }

    // 읽기 전용 — 클래스 레벨 readOnly=true 상속
    public OrderDetailResponse getDetail(Long orderId) {
        return orderRepository.findById(orderId)
            .map(OrderDetailResponse::from)
            .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다: " + orderId));
    }

    // 복합 트랜잭션 — 쓰기 + 이벤트 발행
    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다: " + orderId));
        order.cancel();   // 도메인 메서드에서 상태 검증
        return OrderResponse.from(order);
    }
}
```

> `@Transactional(readOnly = true)` 를 클래스 레벨에 선언하고, 쓰기 메서드에만 `@Transactional`을 추가하는 패턴을 표준으로 한다. `readOnly = true` 는 Hibernate의 dirty-checking 비활성화, 일부 DB의 읽기 전용 복제본 라우팅 등에 유효하다.

---

<a id="6-기술-java-record-사용-dto"></a>

## 6. [기술] Java Record 사용 (DTO)

**Rule**: DTO·설정 값 객체는 Java Record(`record`)를 우선 사용하여 불변성을 보장한다.

```java
// 요청 DTO
public record OrderCreateRequest(
    @NotBlank(message = "상품명은 필수입니다.")
    String productName,

    @Positive(message = "수량은 양수여야 합니다.")
    int quantity
) {}

// 응답 DTO
public record OrderResponse(Long id, String productName, OrderStatus status) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getProductName(), order.getStatus());
    }
}

// 설정
@ConfigurationProperties(prefix = "app.map")
public record MapProperties(
    String kakaoJsAppKey,
    String kakaoMobilityApiKey,
    int timeoutSeconds
) {}
```

---

<a id="7-기능-validation"></a>

## 7. [기능] Validation

**Rule**: 요청 바디는 `@Valid`, 경로·쿼리 파라미터는 `@Validated`를 사용한다. 복잡한 조건은 커스텀 Validator로 분리한다.

### @Valid vs @Validated 사용 위치

| 어노테이션   | 사용 위치                                           | 동작 방식         | 예외                              |
| ------------ | --------------------------------------------------- | ----------------- | --------------------------------- |
| `@Valid`     | `@RequestBody`                                      | Spring MVC가 처리 | `MethodArgumentNotValidException` |
| `@Validated` | Controller 클래스 + `@PathVariable`/`@RequestParam` | Spring AOP가 처리 | `ConstraintViolationException`    |

```java
@Validated   // 클래스에 선언 → 파라미터 검증 활성화
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    // 요청 바디 — @Valid
    @PostMapping
    public OrderResponse create(@Valid @RequestBody OrderCreateRequest req) { ... }

    // 경로 파라미터 — @Validated + 제약 어노테이션
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(
            @PathVariable @Positive(message = "orderId는 양수여야 합니다.") Long orderId) { ... }
}
```

### 커스텀 Validator

```java
// 어노테이션 정의
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface ValidPhoneNumber {
    String message() default "올바른 전화번호 형식이 아닙니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 구현체
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    private static final Pattern PATTERN = Pattern.compile("^010-\\d{4}-\\d{4}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return value != null && PATTERN.matcher(value).matches();
    }
}
```

```java
// 사용
public record MemberCreateRequest(
    @NotBlank String name,
    @ValidPhoneNumber String phone   // 커스텀 validator
) {}
```

### 그룹 검증 (Group Validation)

생성/수정 시 검증 조건이 다를 때 그룹을 활용한다.

```java
public interface OnCreate {}
public interface OnUpdate {}

public record OrderRequest(
    @NotNull(groups = OnCreate.class) Long productId,  // 생성 시에만 필수
    @NotNull(groups = {OnCreate.class, OnUpdate.class}) Integer quantity
) {}

// Controller
@PostMapping
public OrderResponse create(
        @Validated(OnCreate.class) @RequestBody OrderRequest req) { ... }

@PatchMapping("/{id}")
public OrderResponse update(@PathVariable Long id,
        @Validated(OnUpdate.class) @RequestBody OrderRequest req) { ... }
```

---

<a id="8-기능-페이징-처리"></a>

## 8. [기능] 페이징 처리

**Rule**: `Pageable`을 기본으로 사용하되, count 쿼리가 불필요한 무한 스크롤 등에는 `Slice`를 사용한다.

### Page vs Slice 선택 기준

| 타입       | count 쿼리                  | 적합한 경우                             |
| ---------- | --------------------------- | --------------------------------------- |
| `Page<T>`  | **실행됨** (성능 부담 있음) | 전체 페이지 수·총 건수가 UI에 필요할 때 |
| `Slice<T>` | **실행 안 됨**              | 무한 스크롤, "더 보기" 버튼 방식        |

```java
// Page — 전체 건수 포함 (관리자 목록, 페이지 번호 표시)
@GetMapping
public Page<OrderResponse> getOrders(
        @PageableDefault(size = 20, sort = "createdAt", direction = DESC) Pageable pageable) {
    return orderService.search(pageable);
}

// Slice — 다음 페이지 존재 여부만 (무한 스크롤, 모바일 앱)
@GetMapping("/feed")
public Slice<PostResponse> getFeed(
        @PageableDefault(size = 10) Pageable pageable) {
    return postService.getFeed(pageable);
}
```

### count 쿼리 최적화 (JPQL)

```java
@Query(
    value  = "SELECT p FROM Post p WHERE p.status = :status",
    countQuery = "SELECT COUNT(p.id) FROM Post p WHERE p.status = :status"
    // JOIN 없는 단순 count 쿼리로 분리 → 성능 개선
)
Page<Post> findByStatus(@Param("status") PostStatus status, Pageable pageable);
```

### 정렬 허용 필드 제한

외부 입력으로 임의 필드 정렬을 허용하면 보안·성능 위험이 있으므로, 허용 필드를 명시적으로 검증한다.

```java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "title");

private Pageable validateSort(Pageable pageable) {
    pageable.getSort().forEach(order -> {
        if (!ALLOWED_SORT_FIELDS.contains(order.getProperty())) {
            throw new BusinessException("INVALID_SORT_FIELD",
                "정렬 불가 필드: " + order.getProperty(), HttpStatus.BAD_REQUEST);
        }
    });
    return pageable;
}
```

---

<a id="9-api-http-상태-코드-가이드"></a>

## 9. [API] HTTP 상태 코드 가이드

**Rule**: 도메인 상황에 맞는 HTTP 상태 코드를 사용한다. 모든 오류를 400/500으로 뭉개지 않는다.

| 상황                                | HTTP 상태                   | Error Code 예시           |
| ----------------------------------- | --------------------------- | ------------------------- |
| 요청 성공                           | `200 OK`                    | —                         |
| 리소스 생성 성공                    | `201 Created`               | —                         |
| 요청 성공, 응답 없음                | `204 No Content`            | —                         |
| 입력값 형식 오류                    | `400 Bad Request`           | `VALIDATION_ERROR`        |
| 인증 없음 (토큰 없음·만료)          | `401 Unauthorized`          | `UNAUTHORIZED`            |
| 권한 없음 (인증은 됐으나 권한 부족) | `403 Forbidden`             | `FORBIDDEN`               |
| 리소스 없음                         | `404 Not Found`             | `RESOURCE_NOT_FOUND`      |
| 비즈니스 규칙 위반                  | `422 Unprocessable Entity`  | `BUSINESS_RULE_VIOLATION` |
| 중복 생성 시도                      | `409 Conflict`              | `ALREADY_EXISTS`          |
| 서버 내부 오류                      | `500 Internal Server Error` | `INTERNAL_ERROR`          |
| 외부 서비스 오류                    | `502 Bad Gateway`           | `EXTERNAL_SERVICE_ERROR`  |

> `400 Bad Request` vs `422 Unprocessable Entity`: 형식 오류(JSON 파싱, 타입 불일치)는 400, 형식은 맞으나 비즈니스 규칙 위반(잔액 부족, 만료된 쿠폰 등)은 422를 권장한다. 팀 표준이 없으면 400으로 통일해도 무방하다.

### BusinessException에서 상태 코드 매핑 예시

```java
// 서비스에서 도메인 의미를 담아 예외 발생
public OrderResponse create(OrderCreateRequest req) {
    if (orderRepository.existsByProductAndMember(req.productId(), memberId)) {
        throw BusinessException.conflict("이미 동일한 주문이 존재합니다.");   // → 409
    }
    if (!memberService.hasPermission(memberId, "ORDER_CREATE")) {
        throw BusinessException.forbidden("주문 생성 권한이 없습니다.");       // → 403
    }
    // ...
}
```

---

<a id="10-보안-spring-security-인증-인가-cors"></a>

## 10. [보안] Spring Security 인증 인가 CORS

**Rule**: `SecurityFilterChain` Bean 방식 사용(XML 설정 금지), CORS는 `CorsConfigurationSource` Bean으로 환경별 origins를 명확히 관리한다.

### SecurityFilterChain

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS preflight
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .build();
    }
}
```

### CorsConfigurationSource — 환경별 origins 관리

```java
@Configuration
public class CorsConfig {

    // 허용 오리진은 환경 변수에서 주입 — application.yml: cors.allowed-origins: ${CORS_ALLOWED_ORIGINS}
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 오리진 — 환경 변수로 주입 (prod/dev 분리)
        // prod: https://app.example.com
        // dev:  http://localhost:5173, http://localhost:8080
        config.setAllowedOrigins(allowedOrigins);

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
        config.setAllowCredentials(true);   // 쿠키·인증 헤더 포함 시 true
        config.setMaxAge(3600L);            // preflight 캐시 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

```yaml
# application-dev.yml
cors:
  allowed-origins:
    - http://localhost:5173
    - http://localhost:8080

# application-prod.yml
cors:
  allowed-origins:
    - https://app.example.com
```

> `allowCredentials(true)` 와 `allowedOrigins(["*"])` 는 함께 사용할 수 없다.
> 자격증명(쿠키·Authorization 헤더)을 사용한다면 오리진을 반드시 명시적으로 나열해야 한다.> 자격증명(쿠키·Authorization 헤더)을 사용한다면 오리진을 반드시 명시적으로 나열해야 한다.
