# W5-1 Workflow — POST /llm/symptom/analyze 실제 Claude API 연동

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **목표**: symptom-reservation.mustache의 더미 callSymptomApi를 실제 Claude API fetch로 교체

---

## 인터뷰 결과

| 항목 | 결정 |
|------|------|
| Claude 응답 포맷 | 자유 텍스트 → 정규식 파싱 |
| direct-reservation 자동 채움 | 이미 구현 완료 (URL 파라미터 방식) |
| 저장 여부 | 응답만 반환 (LlmRecommendation 저장 불필요) |
| 브랜치 | 현재 `feature/reservation-Llm` 유지 |

---

## 작업 목록 (주석)

```
// [1] llm/dto/SymptomRequest.java 신규
//     - @NotBlank symptomText 필드
//
// [2] llm/dto/SymptomResponse.java 신규
//     - String dept, doctor, time 필드
//
// [3] llm/service/SymptomAnalysisService.java 신규
//     - claudeRestClient 주입 (@RequiredArgsConstructor)
//     - @Value("${claude.api.model}") String model
//     - analyzeSymptom(String symptomText) : SymptomResponse
//       → Claude POST /v1/messages 호출
//       → 응답 content[0].text 에서 "진료과:", "전문의:", "시간:" 파싱
//       → 파싱 실패 시 기본값 반환 (내과 / 의사이영희 / 09:00)
//
// [4] llm/controller/SymptomController.java 신규
//     - @RestController, @RequestMapping("/llm/symptom")
//     - POST /llm/symptom/analyze : @RequestBody SymptomRequest → SymptomResponse
//
// [5] symptom-reservation.mustache 수정
//     - <head>에 CSRF 메타 태그 추가
//     - callSymptomApi 더미(setTimeout) → 실제 fetch('/llm/symptom/analyze') 로 교체
//     - X-CSRF-TOKEN 헤더 포함
```

---

## Step 1: SymptomRequest DTO

**파일**: `src/main/java/com/smartclinic/hms/llm/dto/SymptomRequest.java`

```java
package com.smartclinic.hms.llm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SymptomRequest {
    private String symptomText;
}
```

---

## Step 2: SymptomResponse DTO

**파일**: `src/main/java/com/smartclinic/hms/llm/dto/SymptomResponse.java`

```java
package com.smartclinic.hms.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SymptomResponse {
    private String dept;
    private String doctor;
    private String time;
}
```

---

## Step 3: SymptomAnalysisService

**파일**: `src/main/java/com/smartclinic/hms/llm/service/SymptomAnalysisService.java`

핵심 로직:
- Claude `POST /v1/messages` 호출
- `max_tokens: 200`, `temperature: 0` (결정론적)
- 프롬프트: 진료과/전문의/시간을 지정된 형식으로만 답변 요청
- 응답 `content[0].text` 에서 정규식으로 파싱
- 파싱 실패 시 기본값 `{ "내과", "의사이영희", "09:00" }` 반환

**Claude 요청 body 구조**:
```json
{
  "model": "${claude.api.model}",
  "max_tokens": 200,
  "messages": [
    {
      "role": "user",
      "content": "프롬프트..."
    }
  ]
}
```

**프롬프트**:
```
다음 증상을 분석하고 아래 형식으로만 답변하세요:
진료과: [내과|외과|소아과|이비인후과 중 하나]
전문의: [해당 진료과 의사 1명]
시간: [09:00~15:30 사이 HH:mm]

증상: {symptomText}
```

**파싱 정규식**:
- 진료과: `진료과:\s*(.+)`
- 전문의: `전문의:\s*(.+)`
- 시간: `시간:\s*(\d{2}:\d{2})`

**응답 JSON 역직렬화 대상** (Claude API 응답):
```json
{
  "content": [{ "type": "text", "text": "..." }]
}
```
→ `Map<String, Object>` 또는 전용 내부 record로 역직렬화

---

## Step 4: SymptomController

**파일**: `src/main/java/com/smartclinic/hms/llm/controller/SymptomController.java`

```
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

> POST이므로 `HttpServletRequest` 파라미터 불필요 (`@RequestBody` 사용)

---

## Step 5: symptom-reservation.mustache 수정

**수정 위치 1** — `<head>` 상단에 CSRF 메타 태그 추가:
```html
<meta name="_csrf" content="{{_csrf.token}}">
<meta name="_csrf_header" content="{{_csrf.headerName}}">
```

**수정 위치 2** — `callSymptomApi` 더미 함수 교체:
```js
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
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': csrfToken
    },
    body: JSON.stringify({ symptomText })
  });
  if (!res.ok) throw new Error(`서버 오류: ${res.status}`);
  return res.json();
}
```

> `analyzeSymptom()` 함수와 `SYMPTOM_MAP`은 삭제 (더 이상 불필요)

---

## 완료 기준

- [ ] `SymptomRequest`, `SymptomResponse` DTO 생성
- [ ] `SymptomAnalysisService` 생성 (claudeRestClient 호출 + 파싱)
- [ ] `SymptomController` 생성 (`POST /llm/symptom/analyze`)
- [ ] `symptom-reservation.mustache` CSRF 메타 태그 + fetch 교체
- [ ] `./gradlew build` 오류 없음
- [ ] 수동 테스트: 증상 입력 → `/llm/symptom/analyze` → 결과 화면 표시

---

## 다음 작업

완료 후 → `W5-1-report.md` 작성
