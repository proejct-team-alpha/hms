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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `build.gradle`에 `spring-boot-starter-webflux` 라이브러리를 추가하는 한 줄입니다. 이 라이브러리가 있어야 `WebClient`라는 도구를 사용할 수 있습니다.
> - **왜 이렇게 썼는지**: Java Spring 프로젝트는 외부 라이브러리를 `build.gradle`에 선언해서 가져옵니다. `WebClient`는 Python LLM 서버에 HTTP 요청을 보내기 위한 도구로, WebFlux 의존성을 추가해야 사용할 수 있습니다.
> - **쉽게 말하면**: 앱 스토어에서 앱을 설치하듯, 프로젝트에 필요한 도구(라이브러리)를 목록에 추가하는 것입니다.

### application-dev.properties

```properties
# LLM 서비스 (Python FastAPI)
llm.service.url=${LLM_SERVICE_URL:http://localhost:8000}
llm.service.timeout.connect=5000
llm.service.timeout.read=120000
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Python LLM 서버의 주소와 연결 제한 시간을 설정 파일에 정의합니다. `${LLM_SERVICE_URL:http://localhost:8000}`은 환경변수 `LLM_SERVICE_URL`이 있으면 그 값을 쓰고, 없으면 `http://localhost:8000`을 기본값으로 사용한다는 의미입니다.
> - **왜 이렇게 썼는지**: 서버 주소를 코드에 직접 박지 않고 설정 파일로 분리하면, 배포 환경마다 값을 바꿔도 코드를 다시 빌드할 필요가 없습니다. `timeout.connect=5000`은 5초 안에 연결이 안 되면 포기, `timeout.read=120000`은 응답을 최대 2분까지 기다린다는 의미입니다.
> - **쉽게 말하면**: 전화기에 저장된 연락처처럼, 서버 주소를 코드 밖에 따로 기록해 두어 필요할 때 꺼내 쓰는 방식입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Python LLM 서버에 HTTP 요청을 보내기 위한 `WebClient` 객체를 만들고, 스프링이 관리하는 Bean으로 등록합니다. `@Value`로 설정 파일에서 서버 URL을 읽어 오고, 연결/읽기 타임아웃을 적용한 HttpClient를 내부에 사용합니다.
> - **왜 이렇게 썼는지**: `@Configuration` + `@Bean`을 사용하면 이 `WebClient`를 프로젝트 어디서든 `@Autowired`로 주입받아 재사용할 수 있습니다. 타임아웃을 설정하는 이유는 LLM 모델이 응답을 생성하는 데 시간이 걸리기 때문에, 무한정 기다리지 않도록 제한을 두기 위해서입니다.
> - **쉽게 말하면**: 외부 서버와 통화하기 위한 전용 전화기를 만들어 두고, 회사 전체가 공용으로 쓸 수 있게 비치하는 것과 같습니다.

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
