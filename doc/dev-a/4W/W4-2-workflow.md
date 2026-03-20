# W4-2 Workflow — LLM 연결 기반 설정 이식

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: spring-python-llm-exam-mng의 LLM 연결 설정(의존성, properties, WebClientConfig) HMS 이식

---

## 전체 흐름

```
build.gradle에 WebFlux 의존성 추가
  → application-dev.properties LLM 설정 추가
  → WebClientConfig 이식
  → ./gradlew build 검증
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 이식 대상 | WebFlux 의존성, LLM URL/timeout 설정, WebClientConfig |
| SSE 스트리밍 | 제외 — WebClient 용도만 추가 |
| config/ 패키지 | 수정 허용 (WebClientConfig 신규 추가) |

---

## 실행 흐름

```
[1] build.gradle — spring-boot-starter-webflux 추가
[2] application-dev.properties — llm.service.url, timeout 설정 추가
[3] WebClientConfig.java — config/ 패키지에 이식 (패키지명만 변경)
[4] ./gradlew build — BUILD SUCCESSFUL 검증
```

---

## UI Mockup

```
[설정 작업 — UI 없음]
```

---

## 작업 목록

1. `build.gradle` — `spring-boot-starter-webflux` 의존성 추가
2. `application-dev.properties` — LLM 설정값 추가 (`llm.service.url`, connect/read timeout)
3. `WebClientConfig.java` — `config/` 패키지에 이식 (패키지명만 변경)
4. `./gradlew build` — 오류 없음 검증

---

## 작업 진행내용

- [x] build.gradle 의존성 추가
- [x] application-dev.properties LLM 설정 추가
- [x] WebClientConfig.java 이식
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### build.gradle

```groovy
// WebClient (LLM 연동용, SSE 스트리밍 제외)
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

### application-dev.properties

```properties
# LLM 서비스 (Python FastAPI)
llm.service.url=${LLM_SERVICE_URL:http://localhost:8000}
llm.service.timeout.connect=5000
llm.service.timeout.read=120000
```

### WebClientConfig.java

```java
@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${llm.service.url}")
    private String llmServiceUrl;

    @Bean
    public WebClient llmWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout));

        log.info("LLM WebClient 설정 - URL: {}, connectTimeout: {}ms, readTimeout: {}ms",
                llmServiceUrl, connectTimeout, readTimeout);

        return WebClient.builder()
                .baseUrl(llmServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 빌드 | ./gradlew build | BUILD SUCCESSFUL |
| WebClient Bean | 서버 기동 로그 | "LLM WebClient 설정 - URL: ..." 출력 |

---

## 완료 기준

- [x] `./gradlew build` 오류 없음
- [x] `WebClientConfig` Bean 정상 등록
