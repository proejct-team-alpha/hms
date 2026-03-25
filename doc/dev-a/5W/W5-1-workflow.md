# W5-1 Workflow — POST /llm/symptom/analyze 실제 Claude API 연동

> **작성일**: 5W
> **브랜치**: `feature/reservation-Llm`
> **목표**: symptom-reservation.mustache의 더미 callSymptomApi를 실제 Claude API fetch로 교체

---

## 전체 흐름

```
SymptomRequest / SymptomResponse DTO 신규
  → SymptomAnalysisService 신규 (claudeRestClient → Claude API 호출 + 정규식 파싱)
  → SymptomController 신규 (POST /llm/symptom/analyze)
  → symptom-reservation.mustache 수정 (CSRF 메타태그 + fetch 교체)
  → ./gradlew build 검증
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| Claude 응답 포맷 | 자유 텍스트 → 정규식 파싱 |
| direct-reservation 자동 채움 | 이미 구현 완료 (URL 파라미터 방식) |
| 저장 여부 | 응답만 반환 (LlmRecommendation 저장 불필요) |
| 브랜치 | 현재 `feature/reservation-Llm` 유지 |

---

## 실행 흐름

```
[1] llm/dto/SymptomRequest.java 신규 — symptomText 필드
[2] llm/dto/SymptomResponse.java 신규 — dept, doctor, time 필드
[3] llm/service/SymptomAnalysisService.java 신규
    - claudeRestClient 주입, @Value("${claude.api.model}") model
    - analyzeSymptom() → Claude POST /v1/messages 호출
    - content[0].text 정규식 파싱 (진료과/전문의/시간)
    - 파싱 실패 시 기본값 반환 (내과/의사이영희/09:00)
[4] llm/controller/SymptomController.java 신규 — POST /llm/symptom/analyze
[5] symptom-reservation.mustache 수정
    - <head>에 CSRF 메타태그 추가
    - callSymptomApi 더미 → 실제 fetch 교체
    - SYMPTOM_MAP 로컬 매핑 제거
[6] ./gradlew build 검증
```

---

## UI Mockup

```
[증상 분석 API — UI 직접 변경 없음, 기존 mustache 내부 로직 교체]

symptom-reservation.mustache 변경 전:
┌──────────────────────────────────┐
│  증상 입력 → setTimeout 더미 응답 │
│  SYMPTOM_MAP 로컬 분석           │
└──────────────────────────────────┘

변경 후:
┌──────────────────────────────────┐
│  증상 입력 → POST /llm/symptom/analyze │
│  Claude API 실제 응답 파싱       │
└──────────────────────────────────┘
```

---

## 작업 목록

1. `llm/dto/SymptomRequest.java` 신규 — `symptomText` 필드
2. `llm/dto/SymptomResponse.java` 신규 — `dept`, `doctor`, `time` 필드
3. `llm/service/SymptomAnalysisService.java` 신규 — claudeRestClient 주입, 정규식 파싱, 기본값 반환
4. `llm/controller/SymptomController.java` 신규 — `POST /llm/symptom/analyze`
5. `symptom-reservation.mustache` 수정 — CSRF 메타태그 + fetch 교체, SYMPTOM_MAP 제거
6. `./gradlew build` 검증

---

## 작업 진행내용

- [x] SymptomRequest DTO 생성
- [x] SymptomResponse DTO 생성
- [x] SymptomAnalysisService 생성
- [x] SymptomController 생성
- [x] symptom-reservation.mustache 수정
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### SymptomRequest / SymptomResponse DTO

```java
@Getter @NoArgsConstructor
public class SymptomRequest {
    private String symptomText;
}

@Getter @AllArgsConstructor
public class SymptomResponse {
    private String dept;
    private String doctor;
    private String time;
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 증상 분석 API가 주고받는 데이터의 형태(틀)를 정의합니다. `SymptomRequest`는 사용자가 입력한 증상 텍스트를 담는 틀이고, `SymptomResponse`는 분석 결과인 진료과(`dept`), 의사(`doctor`), 시간(`time`)을 담는 틀입니다.
> - **왜 이렇게 썼는지**: DTO(Data Transfer Object)는 계층 간 데이터를 안전하게 전달하기 위한 객체입니다. `@Getter`는 각 필드를 읽는 메서드를 자동 생성하고, `@NoArgsConstructor`/`@AllArgsConstructor`는 생성자를 자동으로 만들어주는 Lombok 어노테이션입니다.
> - **쉽게 말하면**: "증상을 보내는 봉투(Request)"와 "분석 결과를 받는 봉투(Response)"를 미리 만들어 두는 것과 같습니다.

### SymptomAnalysisService — Claude API 호출 + 파싱

```java
// Claude 요청 body 구조
{
  "model": "${claude.api.model}",
  "max_tokens": 200,
  "messages": [{"role": "user", "content": "프롬프트..."}]
}

// 프롬프트
"다음 증상을 분석하고 아래 형식으로만 답변하세요:\n"
+ "진료과: [내과|외과|소아과|이비인후과 중 하나]\n"
+ "전문의: [해당 진료과 의사 1명]\n"
+ "시간: [09:00~15:30 사이 HH:mm]\n\n"
+ "증상: " + symptomText

// 파싱 정규식
Pattern.compile("진료과:\\s*(.+)");
Pattern.compile("전문의:\\s*(.+)");
Pattern.compile("시간:\\s*(\\d{2}:\\d{2})");
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: Claude AI에게 보낼 요청 구조와 질문(프롬프트)을 정의하고, AI가 돌려준 자유 형식의 텍스트에서 진료과·전문의·시간 정보를 뽑아내는 정규식 패턴을 정의합니다.
> - **왜 이렇게 썼는지**: AI는 자유롭게 텍스트를 반환하기 때문에, 원하는 형식으로만 답변하도록 프롬프트에 명시적으로 지시합니다. 정규식(`Pattern.compile`)은 "진료과: 내과"처럼 특정 패턴의 텍스트를 찾아서 값만 추출하는 도구입니다. `\\s*`는 콜론 뒤의 공백을 허용하는 표현이고, `(.+)`는 이후 내용 전체를 캡처합니다.
> - **쉽게 말하면**: AI한테 "반드시 이 형식으로 대답해"라고 지시하고, 받은 답변에서 필요한 정보만 쏙 뽑아내는 규칙을 만드는 것입니다.

### SymptomController

```java
@RestController
@RequestMapping("/llm/symptom")
@RequiredArgsConstructor
public class SymptomController {
    private final SymptomAnalysisService symptomAnalysisService;

    @PostMapping("/analyze")
    public SymptomResponse analyze(@RequestBody SymptomRequest request) {
        return symptomAnalysisService.analyzeSymptom(request.getSymptomText());
    }
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `POST /llm/symptom/analyze` 주소로 HTTP 요청이 오면 받아서 처리하는 컨트롤러입니다. 요청 본문(JSON)에서 증상 텍스트를 꺼내 분석 서비스에 넘기고, 결과를 JSON으로 응답합니다.
> - **왜 이렇게 썼는지**: `@RestController`는 이 클래스가 REST API 엔드포인트임을 선언합니다. `@RequestMapping("/llm/symptom")`으로 공통 경로를 지정하고, `@PostMapping("/analyze")`로 하위 경로를 추가합니다. `@RequestBody`는 요청 본문의 JSON을 자동으로 `SymptomRequest` 객체로 변환해 줍니다. `@RequiredArgsConstructor`는 `final` 필드를 주입받는 생성자를 자동 생성합니다.
> - **쉽게 말하면**: 손님(클라이언트)이 "/llm/symptom/analyze"라는 창구로 증상을 접수하면, 담당 직원(Service)에게 넘겨주고 결과를 돌려주는 안내 창구 역할입니다.

### symptom-reservation.mustache — CSRF + fetch 교체

```html
<!-- CSRF 메타태그 추가 -->
<meta name="_csrf" content="{{_csrf.token}}">
<meta name="_csrf_header" content="{{_csrf.headerName}}">
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 브라우저가 서버로 POST 요청을 보낼 때 필요한 CSRF 토큰 값을 HTML 헤더 메타태그에 심어 둡니다. 이 값은 서버가 렌더링할 때 `{{_csrf.token}}`이 실제 토큰 값으로 치환됩니다.
> - **왜 이렇게 썼는지**: Spring Security는 기본적으로 CSRF(사이트 간 요청 위조) 공격을 막기 위해 POST 요청에 토큰 검증을 요구합니다. JavaScript fetch로 요청을 보낼 때 이 토큰을 헤더에 포함시켜야 서버가 신뢰할 수 있는 요청으로 받아들입니다. 메타태그에 저장하면 JS에서 `document.querySelector('meta[name="_csrf"]').content`로 쉽게 읽을 수 있습니다.
> - **쉽게 말하면**: 서버한테 "나 진짜 우리 사이트에서 보낸 요청이에요"라는 신분증 번호를 HTML 페이지에 숨겨두는 것입니다.

```javascript
// 변경 전 (더미)
async function callSymptomApi(symptomText) {
  return new Promise(resolve => {
    setTimeout(() => resolve(analyzeSymptom(symptomText)), 1500);
  });
}

// 변경 후 (실제 fetch)
async function callSymptomApi(symptomText) {
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
  const res = await fetch('/llm/symptom/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': csrfToken },
    body: JSON.stringify({ symptomText })
  });
  if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
  return res.json();
}
// analyzeSymptom() 함수와 SYMPTOM_MAP 삭제
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 변경 전에는 1.5초 후 미리 정해둔 답변을 주는 가짜(더미) 함수였고, 변경 후에는 실제 서버의 `/llm/symptom/analyze` 엔드포인트에 증상 텍스트를 POST로 전송하고 AI 분석 결과를 받아옵니다.
> - **왜 이렇게 썼는지**: `async/await`는 비동기 작업(서버 통신처럼 시간이 걸리는 작업)을 순서대로 읽히도록 작성하는 방식입니다. `fetch`는 브라우저 내장 HTTP 요청 함수입니다. `?.content || ''`는 메타태그가 없을 경우 빈 문자열로 대체하는 안전 처리입니다. `res.json()`은 서버 응답을 JavaScript 객체로 변환합니다.
> - **쉽게 말하면**: 예전에는 "항상 이 답변 줄게"라는 가짜 직원이었는데, 이제는 실제로 AI 서버에 물어보고 결과를 가져오는 진짜 직원으로 교체한 것입니다.

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 빌드 | ./gradlew build | BUILD SUCCESSFUL |
| POST /llm/symptom/analyze | 증상 텍스트 입력 | dept, doctor, time JSON 반환 |
| 파싱 실패 | Claude 응답 포맷 불일치 | 기본값 반환 (내과/의사이영희/09:00) |
| 수동 테스트 | 증상 입력 → 결과 화면 표시 | direct-reservation 자동 채움 동작 |

---

## 완료 기준

- [x] `SymptomRequest`, `SymptomResponse` DTO 생성
- [x] `SymptomAnalysisService` 생성 (claudeRestClient 호출 + 파싱)
- [x] `SymptomController` 생성 (`POST /llm/symptom/analyze`)
- [x] `symptom-reservation.mustache` CSRF 메타태그 + fetch 교체
- [x] `./gradlew build` 오류 없음
