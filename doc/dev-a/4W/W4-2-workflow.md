# W4-2 LLM 연결 기반 설정 이식

## 작업 목표
`spring-python-llm-exam-mng`의 LLM 연결 설정(의존성, properties, WebClientConfig)을 HMS에 이식한다.
SSE 스트리밍은 제외, WebClient 용도만 추가한다.

## 작업 목록

1. `build.gradle` — `spring-boot-starter-webflux` 의존성 추가
2. `application-dev.properties` — LLM 서비스 설정값 추가 (`llm.service.url`, timeout)
3. `WebClientConfig.java` — `config/` 패키지에 이식 (패키지명만 변경)
4. `./gradlew build` — 오류 없음 검증

## 진행 현황
- [x] 1. build.gradle 의존성 추가
- [x] 2. application-dev.properties LLM 설정 추가
- [x] 3. WebClientConfig.java 이식
- [x] 4. 빌드 확인 — BUILD SUCCESSFUL

## 수정/추가 파일
- `build.gradle`
- `src/main/resources/application-dev.properties`
- `src/main/java/com/smartclinic/hms/config/WebClientConfig.java` (신규)

## 상세 내용

### 1. build.gradle 추가
```groovy
// WebClient (LLM 연동용, SSE 스트리밍 제외)
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```
> Spring Boot Starters 알파벳 순 블록에 추가

### 2. application-dev.properties 추가
```properties
# ------------------------------------------------------------------------------
# LLM 서비스 (Python FastAPI)
# ------------------------------------------------------------------------------
llm.service.url=${LLM_SERVICE_URL:http://localhost:8000}
llm.service.timeout.connect=5000
llm.service.timeout.read=120000
```

### 3. WebClientConfig.java (이식)
- 원본: `com.sample.llm.config.WebClientConfig`
- 대상: `com.smartclinic.hms.config.WebClientConfig`
- 패키지명 외 코드 변경 없음

```java
package com.smartclinic.hms.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${llm.service.url}")
    private String llmServiceUrl;

    @Value("${llm.service.timeout.connect}")
    private int connectTimeout;

    @Value("${llm.service.timeout.read}")
    private int readTimeout;

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

## 수용 기준
- [ ] `./gradlew build` 오류 없음
- [ ] `WebClientConfig` Bean 정상 등록 (로그 확인: "LLM WebClient 설정 - URL: ...")
