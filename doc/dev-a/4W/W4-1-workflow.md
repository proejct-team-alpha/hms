# W4-1 Workflow — HMS 패키지 구조 확인 및 Entity 스키마 비교

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: HMS 패키지 구조 파악 + 중복 Entity 스키마 비교 + 통합 전략 확정

---

## 전체 흐름

```
HMS 패키지 구조 파악 → domain Entity 스키마 확인
  → spring-python-llm 참조 Entity 확인
  → 중복 Entity 비교 → 통합 전략 결정
  → Task 2 진행 준비
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 목적 | spring-python-llm-exam-mng LLM 기능을 HMS에 병합 |
| 충돌 Entity | ChatHistory vs ChatbotHistory, MedicalRule vs HospitalRule, Reservation |
| 전략 | HMS 기준 유지, spring-llm 누락 필드 추가 |
| LLM 전용 Entity | MedicalHistory, MedicalContent, MedicalQa, MedicalDomain 신규 추가 예정 |

---

## 실행 흐름

```
Step 1: HMS 패키지 구조 확인 (com.smartclinic.hms 하위)
Step 2: HMS domain Entity 스키마 확인 (6개 Entity)
Step 3: spring-llm 참조 Entity 확인 (ChatHistory, MedicalRule, Reservation)
Step 4: 중복 Entity 비교 및 통합 전략 결정
Step 5: LLM 전용 신규 Entity 목록 확인
Step 6: HMS llm 패키지 현황 확인
```

---

## UI Mockup

```
[분석 작업 — UI 없음]
```

---

## 작업 목록

1. HMS `src/main/java/com/smartclinic/hms/` 디렉토리 구조 파악
2. HMS domain Entity 6개 스키마 확인 (ChatbotHistory, HospitalRule, Reservation, Doctor, Staff, DoctorSchedule)
3. spring-llm Entity 스키마 확인 (ChatHistory, MedicalRule, Reservation)
4. 중복 Entity 비교 및 통합 전략 문서화
5. LLM 전용 신규 Entity 목록 확정
6. HMS llm 패키지 현황 파악

---

## 작업 진행내용

- [x] HMS 패키지 구조 확인
- [x] domain Entity 스키마 비교
- [x] spring-llm Entity 확인
- [x] 통합 전략 확정
- [x] LLM 전용 Entity 목록 확정

---

## 실행 흐름에 대한 코드

### 통합 전략 결정표

| Entity | 전략 |
|--------|------|
| `ChatHistory` vs `ChatbotHistory` | HMS `ChatbotHistory` 사용 |
| `MedicalRule` vs `HospitalRule` | HMS `HospitalRule` 기준 + target/startDate/endDate 추가 |
| `Reservation` | HMS 기준 + startTime/endTime 추가 |
| `Doctor`, `Staff` | HMS 것 사용 |
| `DoctorSchedule` | HMS에 없음 → 신규 추가 |
| `MedicalHistory`, `MedicalContent`, `MedicalQa`, `MedicalDomain` | 신규 추가 |

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 분석 작업 | 패키지 구조 확인 | 구조 파악 완료 |
| 통합 전략 | Entity 비교 | 통합 전략 확정 |

---

## 완료 기준

- [x] HMS 패키지 구조 전체 파악
- [x] 6개 중복 Entity 비교 및 통합 전략 확정
- [x] LLM 전용 신규 Entity 목록 확정
- [x] HMS llm 패키지 현황 파악
- [x] Task 2 진행 준비 완료
