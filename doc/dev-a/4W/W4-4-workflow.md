# W4-4 Workflow — Entity 및 Repository 이식

> **작성일**: 4W
> **브랜치**: `feature/Llm`
> **목표**: spring-python-llm Entity/Repository를 HMS domain 패키지로 이식

---

## 전체 흐름

```
HMS 기존 Entity 확장 (HospitalRule, Reservation)
  → LLM 전용 신규 Entity 5개 추가
  → Repository 6개 신규 추가
  → ./gradlew build JPA 스캔 오류 없음 검증
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| 중복 Entity | HMS 기준 유지, 누락 필드 추가 |
| HospitalRule 추가 필드 | target, startDate, endDate |
| Reservation 추가 필드 | startTime, endTime |
| 신규 Entity | DoctorSchedule, MedicalHistory, MedicalContent, MedicalQa, MedicalDomain |
| domain/ 패키지 | 금지 영역이나 이번 Task에서 수정 허용 |

---

## 실행 흐름

```
[1] HospitalRule — target, startDate, endDate 필드 추가
[2] Reservation — startTime, endTime 필드 추가
[3] DoctorSchedule — 신규 (Doctor @ManyToOne)
[4] MedicalHistory — 신규 (Staff @ManyToOne)
[5] MedicalContent, MedicalQa, MedicalDomain — 신규 (FK 없음)
[6] ChatbotHistoryRepository — 신규 (domain/)
[7] LLM Repository 5개 — 신규
[8] ./gradlew build 검증
```

---

## UI Mockup

```
[Entity 이식 작업 — UI 없음]
```

---

## 작업 목록

1. `HospitalRule.java` — `target`, `startDate`, `endDate` 필드 추가
2. `Reservation.java` — `startTime`, `endTime` 필드 추가
3. `DoctorSchedule.java` — 신규 Entity (Doctor 단방향 @ManyToOne)
4. `MedicalHistory.java` — 신규 Entity (Staff 단방향 @ManyToOne)
5. `MedicalContent.java`, `MedicalQa.java`, `MedicalDomain.java` — 신규 Entity
6. `ChatbotHistoryRepository.java` — 신규 (findByStaffIdOrderByCreatedAtDesc)
7. LLM Repository 5개 신규 — DoctorSchedule, MedicalHistory, MedicalContent, MedicalQa, MedicalDomain
8. `./gradlew build` — JPA Entity 스캔 오류 없음 검증

---

## 작업 진행내용

- [x] HospitalRule 필드 확장
- [x] Reservation 필드 확장
- [x] DoctorSchedule 신규
- [x] MedicalHistory 신규
- [x] MedicalContent, MedicalQa, MedicalDomain 신규
- [x] ChatbotHistoryRepository 신규
- [x] LLM Repository 5개 신규
- [x] 빌드 확인 — BUILD SUCCESSFUL

---

## 실행 흐름에 대한 코드

### HospitalRule — 추가 필드

```java
@Column(length = 100)
private String target;

@Column(name = "start_date")
private LocalDate startDate;

@Column(name = "end_date")
private LocalDate endDate;
```

### Reservation — 추가 필드

```java
@Column(name = "start_time")
private LocalTime startTime;

@Column(name = "end_time")
private LocalTime endTime;
```

### ChatbotHistoryRepository

```java
public interface ChatbotHistoryRepository extends JpaRepository<ChatbotHistory, Long> {
    List<ChatbotHistory> findByStaffIdOrderByCreatedAtDesc(Long staffId);
}
```

### DoctorScheduleRepository

```java
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorIdAndIsAvailableTrue(Long doctorId);
    List<DoctorSchedule> findByDoctor_IdAndDayOfWeek(Long doctorId, String dayOfWeek);
}
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 빌드 | ./gradlew build | BUILD SUCCESSFUL |
| JPA 스캔 | 서버 기동 | 중복 테이블 매핑 없음 |

---

## 완료 기준

- [x] `./gradlew build` 오류 없음
- [x] JPA Entity 스캔 정상 (중복 테이블 매핑 없음)
