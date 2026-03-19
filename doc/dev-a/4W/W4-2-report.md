# W4-2 리포트 - LLM 연결 기반 설정 이식

## 작업 개요
- **작업명**: spring-python-llm-exam-mng의 LLM 연결 설정을 HMS에 이식
- **수정/추가 파일**:
  - `build.gradle`
  - `src/main/resources/application-dev.properties`
  - `src/main/java/com/smartclinic/hms/config/WebClientConfig.java` (신규)

## 작업 내용

### 1. build.gradle — webflux 의존성 추가
WebClient 사용을 위해 `spring-boot-starter-webflux`를 추가했다. SSE 스트리밍은 이번 작업 범위 밖이며, Python LLM 서버 호출 용도로만 사용한다.

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```

### 2. application-dev.properties — LLM 서비스 설정 추가
Python FastAPI LLM 서버 연결 설정을 추가했다. URL은 환경변수 `LLM_SERVICE_URL`로 오버라이드 가능하며, 기본값은 `http://localhost:8000`이다.

```properties
llm.service.url=${LLM_SERVICE_URL:http://localhost:8000}
llm.service.timeout.connect=5000
llm.service.timeout.read=120000
```

| 설정 키 | 값 | 설명 |
|---|---|---|
| `llm.service.url` | `http://localhost:8000` | Python LLM 서버 주소 |
| `llm.service.timeout.connect` | 5000ms | 연결 타임아웃 |
| `llm.service.timeout.read` | 120000ms | 읽기 타임아웃 (LLM 추론 시간 고려) |

### 3. WebClientConfig.java — config/ 패키지에 이식
원본(`com.sample.llm.config`)에서 패키지명만 `com.smartclinic.hms.config`로 변경하여 이식했다. `llmWebClient` Bean은 `llm.service.url`을 baseUrl로, Netty HttpClient에 connect/read 타임아웃을 적용한다.

## 빌드 결과

```
BUILD SUCCESSFUL in 9s
```

## 특이사항
- IDE에서 `@Bean` 메서드 `public` 접근제어자에 대한 Hint가 표시되나 컴파일/빌드 오류 아님
- `spring-boot-starter-webflux`와 `spring-boot-starter-web` 공존 구조 — WebClient만 사용하므로 MVC/WebFlux 혼용 충돌 없음
- `llm.service.timeout.read=120000ms`(2분)는 Ollama 7B 모델 추론 시간 기준 설정값
