<!-- Parent: ../AI-CONTEXT.md -->

# domain — JPA 엔티티

## 목적

HMS 전체에서 공유되는 JPA 엔티티 모음. **책임개발자 단독 소유.**
엔티티 수정은 전체 모듈에 영향을 주므로 반드시 이슈 등록 후 처리한다.

## 주요 파일

| 파일 | 설명 |
|------|------|
| Staff.java | 직원 (username, password, role, department) |
| StaffRole.java | ADMIN / STAFF / DOCTOR / NURSE Enum |
| Doctor.java | 의사 (staff 1:1, department, availableDays, specialty) |
| Patient.java | 환자 (name, phone — 비회원 식별 키) |
| Reservation.java | 예약 (patient, doctor, date, timeSlot, status, source, reservationNumber) |
| ReservationStatus.java | RESERVED → RECEIVED → COMPLETED / CANCELLED Enum |
| ReservationSource.java | ONLINE / PHONE / WALKIN Enum |
| Department.java | 진료과 (name, isActive) |
| TreatmentRecord.java | 진료 기록 (reservation, notes) |
| Item.java | 물품 (name, category, quantity) |
| ItemCategory.java | 물품 카테고리 Enum |
| HospitalRule.java | 병원 규칙 (title, content, category) |
| HospitalRuleCategory.java | 규칙 카테고리 Enum |
| LlmRecommendation.java | LLM 추천 이력 (symptomText, recommendedDept 등) |
| ChatbotHistory.java | 챗봇 대화 이력 |

## 핵심 관계

```
Department 1 ─── N Doctor
Staff     1 ─── 1 Doctor
Patient   1 ─── N Reservation
Doctor    1 ─── N Reservation
Reservation 1 ─── 1 TreatmentRecord
```

## 예약 상태 전이

```
RESERVED → RECEIVED (Staff 접수)
RECEIVED → COMPLETED (Doctor 진료완료)
RESERVED/RECEIVED → CANCELLED
```

## AI 작업 지침

- 엔티티 필드/관계 추가·삭제는 **이슈 등록 후 책임개발자 처리**
- LAZY 로딩 기본 — 연관 엔티티 접근 시 `JOIN FETCH` 또는 `@Transactional` 필수
- 엔티티에 비즈니스 로직 추가 금지 (순수 데이터 모델)
- Doctor.availableDays: `"MON,WED,FRI"` 쉼표 구분 문자열

## 의존성

- 외부: JPA, Hibernate, Lombok
