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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `build.gradle`에 `spring-boot-starter-webflux` 라이브러리를 추가합니다. 이 한 줄로 `WebClient`를 사용할 수 있게 됩니다.
> - **왜 이렇게 썼는지**: Gradle 빌드 도구는 `implementation`으로 선언된 라이브러리를 외부 저장소(Maven Central)에서 자동으로 내려받아 프로젝트에 포함시킵니다. WebFlux 라이브러리가 없으면 `WebClient` 클래스 자체를 import할 수 없습니다.
> - **쉽게 말하면**: 요리에 필요한 재료를 장바구니에 담듯, 프로젝트에 필요한 도구(라이브러리)를 목록에 추가하는 것입니다.

### 2. application-dev.properties — LLM 서비스 설정 추가
Python FastAPI LLM 서버 연결 설정을 추가했다. URL은 환경변수 `LLM_SERVICE_URL`로 오버라이드 가능하며, 기본값은 `http://localhost:8000`이다.

```properties
llm.service.url=${LLM_SERVICE_URL:http://localhost:8000}
llm.service.timeout.connect=5000
llm.service.timeout.read=120000
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Python LLM 서버의 주소(`url`)와 연결 제한 시간(`timeout`)을 설정 파일에 기록합니다. `${LLM_SERVICE_URL:http://localhost:8000}`은 환경변수가 있으면 그 값을, 없으면 `http://localhost:8000`을 기본값으로 씁니다.
> - **왜 이렇게 썼는지**: 서버 주소를 코드 안에 직접 쓰면 서버가 바뀔 때마다 코드를 수정하고 다시 빌드해야 합니다. 설정 파일로 분리하면 배포 환경만 바꿔도 동작합니다. `timeout.read=120000`(2분)은 LLM 모델이 답변을 생성하는 데 최대 2분까지 걸릴 수 있어 여유 있게 설정한 값입니다.
> - **쉽게 말하면**: 내비게이션 앱에서 목적지 주소를 코드에 박지 않고 사용자가 입력할 수 있도록 하는 것과 같은 원리입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `./gradlew build` 명령 실행 결과로, 프로젝트의 모든 Java 코드가 오류 없이 컴파일되고 테스트까지 통과했음을 나타냅니다.
> - **왜 이렇게 썼는지**: 빌드 성공 여부는 이식 작업이 기존 코드와 충돌하지 않았음을 확인하는 가장 기본적인 검증 방법입니다.
> - **쉽게 말하면**: 새 부품을 달고 나서 시동이 걸리는지 확인하는 것과 같습니다.

## 특이사항
- IDE에서 `@Bean` 메서드 `public` 접근제어자에 대한 Hint가 표시되나 컴파일/빌드 오류 아님
- `spring-boot-starter-webflux`와 `spring-boot-starter-web` 공존 구조 — WebClient만 사용하므로 MVC/WebFlux 혼용 충돌 없음
- `llm.service.timeout.read=120000ms`(2분)는 Ollama 7B 모델 추론 시간 기준 설정값
