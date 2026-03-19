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
