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

### symptom-reservation.mustache — CSRF + fetch 교체

```html
<!-- CSRF 메타태그 추가 -->
<meta name="_csrf" content="{{_csrf.token}}">
<meta name="_csrf_header" content="{{_csrf.headerName}}">
```

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
