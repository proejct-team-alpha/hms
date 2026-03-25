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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 기존 `HospitalRule` Entity(병원 규칙 테이블)에 세 개의 컬럼을 추가합니다. `target`은 규칙 적용 대상(예: "전 직원"), `startDate`와 `endDate`는 규칙의 유효 기간입니다.
> - **왜 이렇게 썼는지**: `@Column` 어노테이션은 이 Java 필드가 데이터베이스 테이블의 어떤 컬럼과 연결되는지 JPA(Java-DB 연결 도구)에게 알려줍니다. `name = "start_date"`처럼 명시하면 DB 컬럼명을 직접 지정할 수 있습니다. `LocalDate`는 날짜만 다루는 타입입니다.
> - **쉽게 말하면**: 기존 병원 규칙 표에 새로운 열(컬럼)을 추가하는 것으로, 이제 규칙마다 언제부터 언제까지, 누구에게 적용되는지 기록할 수 있습니다.

### Reservation — 추가 필드

```java
@Column(name = "start_time")
private LocalTime startTime;

@Column(name = "end_time")
private LocalTime endTime;
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 기존 `Reservation` Entity(예약 테이블)에 예약 시작 시간과 종료 시간 컬럼을 추가합니다.
> - **왜 이렇게 썼는지**: 기존 HMS는 예약 시간을 `timeSlot`이라는 문자열(예: "09:00")로 관리했습니다. LLM 슬롯 조회 기능에서 시간 비교가 필요하여 `LocalTime` 타입의 정확한 시간 필드를 추가했습니다. `LocalTime`은 시간과 분만 다루는 타입입니다.
> - **쉽게 말하면**: 예약 테이블에 "시작 시간"과 "끝 시간" 열을 추가해서, AI가 빈 시간대를 계산할 수 있도록 합니다.

### ChatbotHistoryRepository

```java
public interface ChatbotHistoryRepository extends JpaRepository<ChatbotHistory, Long> {
    List<ChatbotHistory> findByStaffIdOrderByCreatedAtDesc(Long staffId);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `ChatbotHistory`(챗봇 대화 기록) 테이블을 조회하는 Repository(저장소) 인터페이스를 정의합니다. `findByStaffIdOrderByCreatedAtDesc`는 특정 직원(`staffId`)의 대화 기록을 최신 순으로 가져오는 메서드입니다.
> - **왜 이렇게 썼는지**: Spring Data JPA는 메서드 이름만 규칙에 맞게 작성하면 SQL을 직접 쓰지 않아도 자동으로 쿼리를 생성해 줍니다. `findBy직원Id`, `OrderBy생성일시Desc`처럼 영어 단어를 조합하면 됩니다. `JpaRepository<ChatbotHistory, Long>`을 상속하면 기본 저장/조회/삭제 메서드도 자동으로 제공됩니다.
> - **쉽게 말하면**: 데이터베이스에서 "이 직원의 챗봇 대화 기록을 최신순으로 가져와"라는 명령을 메서드 이름만으로 표현한 것입니다.

### DoctorScheduleRepository

```java
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorIdAndIsAvailableTrue(Long doctorId);
    List<DoctorSchedule> findByDoctor_IdAndDayOfWeek(Long doctorId, String dayOfWeek);
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 의사 스케줄 테이블을 조회하는 Repository입니다. 첫 번째 메서드는 특정 의사의 예약 가능한 스케줄만, 두 번째는 특정 의사의 특정 요일 스케줄을 가져옵니다.
> - **왜 이렇게 썼는지**: `findByDoctorIdAndIsAvailableTrue`는 `doctorId`가 일치하고 `isAvailable`이 `true`인 데이터를 찾는 쿼리를 자동 생성합니다. `findByDoctor_IdAndDayOfWeek`에서 `_`는 연관 관계(Doctor Entity의 id 필드)를 탐색할 때 사용하는 Spring Data JPA 표기법입니다.
> - **쉽게 말하면**: "이 의사가 진료 가능한 날짜 목록을 가져와" 또는 "이 의사의 월요일 스케줄을 가져와"처럼 다양한 조건으로 데이터를 검색하는 기능입니다.

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
