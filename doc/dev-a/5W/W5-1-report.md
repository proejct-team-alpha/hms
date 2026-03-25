# W5-1 Report — POST /llm/symptom/analyze 실제 Claude API 연동

> **작성일**: 2026-03-19
> **브랜치**: `feature/reservation-Llm`
> **빌드**: BUILD SUCCESSFUL

---

## 작업 완료 목록

| # | 항목 | 상태 |
|---|------|------|
| 1 | `llm/dto/SymptomRequest.java` 신규 | ✅ |
| 2 | `llm/dto/SymptomResponse.java` 신규 | ✅ |
| 3 | `llm/service/SymptomAnalysisService.java` 신규 | ✅ |
| 4 | `llm/controller/SymptomController.java` 신규 | ✅ |
| 5 | `symptom-reservation.mustache` CSRF 메타 태그 추가 | ✅ |
| 6 | `symptom-reservation.mustache` callSymptomApi 실제 fetch 교체 | ✅ |
| 7 | SYMPTOM_MAP / analyzeSymptom 더미 코드 제거 | ✅ |
| 8 | `./gradlew build` 성공 | ✅ |

---

## 생성/수정 파일

### 신규 파일

- `src/main/java/com/smartclinic/hms/llm/dto/SymptomRequest.java`
- `src/main/java/com/smartclinic/hms/llm/dto/SymptomResponse.java`
- `src/main/java/com/smartclinic/hms/llm/service/SymptomAnalysisService.java`
- `src/main/java/com/smartclinic/hms/llm/controller/SymptomController.java`

### 수정 파일

- `src/main/resources/templates/reservation/symptom-reservation.mustache`
  - `<head>` CSRF 메타 태그 추가
  - `callSymptomApi` 더미(setTimeout) → `fetch('/llm/symptom/analyze')` 교체
  - `SYMPTOM_MAP`, `analyzeSymptom()` 더미 코드 제거

---

## 주요 결정 사항

| 항목 | 결정 |
|------|------|
| Claude 응답 파싱 | 자유 텍스트 → 정규식 (`진료과:`, `전문의:`, `시간:`) |
| 파싱 실패 폴백 | `{ 내과, 의사이영희, 09:00 }` 기본값 반환 (throw 대신) |
| 저장 여부 | 미저장 (응답만 반환) |
| CSRF | `/llm/symptom/**` SecurityConfig에 CSRF ignore 설정 완료 (W4-6), 추가 설정 불필요 |
| RestClient 역직렬화 | `ParameterizedTypeReference<Map<String,Object>>` 사용 |

> **💡 입문자 설명**
>
> **정규식(Regex)으로 Claude 응답 파싱 — 왜 이 방법인지**
> - Claude는 자연어 텍스트로 응답합니다. "진료과: 내과, 전문의: 이영희, 시간: 09:00" 같은 형태로 오는데, 이를 Java 코드에서 쓸 수 있는 구조화된 데이터로 변환해야 합니다.
> - 정규식은 텍스트에서 특정 패턴("진료과:" 뒤에 오는 텍스트)을 찾아내는 도구입니다. JSON으로 고정 응답을 받는 것보다 유연하지만, 응답 형식이 조금 달라지면 파싱이 실패할 수 있는 단점도 있습니다.
> - **다른 방법**: Claude에게 JSON 형식으로만 응답하도록 프롬프트를 작성하고 `ObjectMapper`로 파싱할 수도 있습니다. 더 안정적이지만 프롬프트 설계가 복잡해집니다.
>
> **파싱 실패 시 예외 대신 기본값 반환 — 왜 throw하지 않는지**
> - AI 응답은 예측 불가합니다. 파싱이 실패할 때마다 오류 화면을 보여주면 사용자 경험이 나빠집니다.
> - 기본값(`내과, 의사이영희, 09:00`)을 반환해 사용자가 예약 폼을 직접 수정할 수 있게 하는 것이 더 실용적입니다. AI 추천은 보조 수단이지 필수가 아니기 때문입니다.
> - **다른 방법**: 파싱 실패 시 "다시 시도해주세요" 메시지를 반환할 수도 있지만, 사용자가 직접 입력할 수 있는 폼이 있으므로 기본값 방식이 더 자연스럽습니다.
>
> **`ParameterizedTypeReference<Map<String,Object>>` — 왜 이게 필요한지**
> - Java는 제네릭 타입 정보(예: `Map<String,Object>`)를 런타임에 잃어버리는 "타입 소거" 문제가 있습니다. `ParameterizedTypeReference`는 이 타입 정보를 런타임에도 유지시켜 JSON 역직렬화가 올바르게 동작하도록 합니다.
> - **쉽게 말하면**: "이 JSON을 `Map<String,Object>` 형태로 변환해줘"라는 타입 힌트를 RestClient에게 명시적으로 알려주는 도우미 클래스입니다.
>
> **CSRF ignore — 왜 LLM 엔드포인트에만 적용하는지**
> - CSRF 보호는 브라우저 폼 기반 요청에 필요합니다. `/llm/symptom/analyze`는 JavaScript `fetch()`로 호출되는 API 엔드포인트라 CSRF 토큰을 HTTP 헤더로 보내는 방식이 적합하지만, 이미 W4-6에서 예외 처리가 완료됐습니다. 일반 폼 제출 엔드포인트는 여전히 CSRF 보호가 유지됩니다.

---

## SecurityConfig 기존 설정 재확인

`/llm/symptom/**`는 W4-6에서 이미 처리됨:
- CSRF ignore: 적용
- 접근 권한: `permitAll()`

---

## 수동 테스트 시나리오

1. `/reservation/symptom-reservation` 접속
2. 증상 입력 (예: "어제부터 열이 나고 목이 아파요")
3. "증상 분석하기" 클릭
4. Claude API 호출 → 진료과/전문의/시간 추천 결과 표시
5. "이 정보로 예약 진행하기" → `/reservation/direct-reservation?dept=...&doctor=...&time=...`
6. direct-reservation 폼에 자동 채움 확인

> python-llm 서버(192.168.x.73:8000)와 무관 — Claude REST API 직접 호출
