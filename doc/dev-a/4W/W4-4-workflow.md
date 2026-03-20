# W4-4 Entity 및 Repository 이식

## 작업 목표
spring-python-llm-exam-mng의 Entity/Repository를 HMS domain 패키지로 이식한다.
- 중복 Entity(ChatHistory, Doctor, Staff)는 HMS 것을 사용
- HospitalRule, Reservation은 누락 필드 확장
- LLM 전용 Entity 5개 신규 추가
- Repository 6개 신규 추가 (domain/ 패키지)

## 작업 목록

1. `HospitalRule` 확장 — `target`(String), `startDate`(LocalDate), `endDate`(LocalDate) 필드 추가
2. `Reservation` 확장 — `startTime`(LocalTime), `endTime`(LocalTime) 필드 추가
3. `DoctorSchedule` 신규 Entity — HMS `Doctor` 단방향 `@ManyToOne` 참조
4. `MedicalHistory` 신규 Entity — HMS `Staff` 단방향 `@ManyToOne` 참조
5. `MedicalContent` 신규 Entity
6. `MedicalQa` 신규 Entity
7. `MedicalDomain` 신규 Entity (`@Id`: `domainId`, Integer)
8. `ChatbotHistoryRepository` 신규 — `domain/` 패키지
9. LLM Entity Repository 5개 신규 — `DoctorScheduleRepository`, `MedicalHistoryRepository`, `MedicalContentRepository`, `MedicalQaRepository`, `MedicalDomainRepository`
10. `./gradlew build` — JPA Entity 스캔 오류 없음 검증

## 진행 현황
- [x] 1. HospitalRule 필드 확장
- [x] 2. Reservation 필드 확장
- [x] 3. DoctorSchedule 신규
- [x] 4. MedicalHistory 신규
- [x] 5. MedicalContent 신규
- [x] 6. MedicalQa 신규
- [x] 7. MedicalDomain 신규
- [x] 8. ChatbotHistoryRepository 신규
- [x] 9. LLM Repository 5개 신규
- [x] 10. 빌드 확인 — BUILD SUCCESSFUL

## 수정/추가 파일
**수정**
- `src/main/java/com/smartclinic/hms/domain/HospitalRule.java`
- `src/main/java/com/smartclinic/hms/domain/Reservation.java`

**신규 Entity**
- `src/main/java/com/smartclinic/hms/domain/DoctorSchedule.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalHistory.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalContent.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalQa.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalDomain.java`

**신규 Repository**
- `src/main/java/com/smartclinic/hms/domain/ChatbotHistoryRepository.java`
- `src/main/java/com/smartclinic/hms/domain/DoctorScheduleRepository.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalHistoryRepository.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalContentRepository.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalQaRepository.java`
- `src/main/java/com/smartclinic/hms/domain/MedicalDomainRepository.java`

---

## 상세 내용

### 스키마 비교표

#### HospitalRule vs MedicalRule
| 필드 | HospitalRule (HMS) | MedicalRule (spring-llm) | 처리 |
|---|---|---|---|
| id | Long | Integer | HMS 유지 |
| title | String(200) | String(200) | HMS 유지 |
| content | TEXT | LONGTEXT | HMS 유지 |
| category | HospitalRuleCategory(enum) | String(50) | HMS enum 유지 |
| active | boolean | 없음 | HMS 유지 |
| createdAt | LocalDateTime | LocalDateTime | HMS 유지 |
| updatedAt | LocalDateTime | 없음 | HMS 유지 |
| **target** | **없음** | **String(100)** | **HMS에 추가** |
| **startDate** | **없음** | **LocalDate** | **HMS에 추가** |
| **endDate** | **없음** | **LocalDate** | **HMS에 추가** |

#### Reservation 비교
| 필드 | HMS Reservation | spring-llm Reservation | 처리 |
|---|---|---|---|
| id | Long | Integer | HMS 유지 |
| reservationNumber | String(25) | 없음 | HMS 유지 |
| patient | @ManyToOne Patient | 없음 | HMS 유지 |
| doctor | @ManyToOne Doctor | @ManyToOne Doctor | HMS 유지 |
| department | @ManyToOne Department | 없음 | HMS 유지 |
| staff | 없음 | @ManyToOne Staff | 불필요(HMS 구조상) |
| reservationDate | LocalDate | LocalDate | HMS 유지 |
| timeSlot | String(10) | 없음 | HMS 유지 |
| **startTime** | **없음** | **LocalTime** | **HMS에 추가** |
| **endTime** | **없음** | **LocalTime** | **HMS에 추가** |
| status | ReservationStatus(enum) | String | HMS enum 유지 |
| source | ReservationSource(enum) | 없음 | HMS 유지 |

---

### 1. HospitalRule — 추가 필드
```java
@Column(length = 100)
private String target;

@Column(name = "start_date")
private LocalDate startDate;

@Column(name = "end_date")
private LocalDate endDate;
```

### 2. Reservation — 추가 필드
```java
@Column(name = "start_time")
private LocalTime startTime;

@Column(name = "end_time")
private LocalTime endTime;
```

### 3. DoctorSchedule (신규)
- `Doctor` → `com.smartclinic.hms.domain.Doctor` 참조
- 단방향 `@ManyToOne`만 유지 (Doctor에 @OneToMany 추가 없음)

### 4. MedicalHistory (신규)
- `Staff` → `com.smartclinic.hms.domain.Staff` 참조
- 단방향 `@ManyToOne`만 유지

### 5~7. MedicalContent, MedicalQa, MedicalDomain (신규)
- FK 없음, 패키지명만 변경하여 이식

### 8. ChatbotHistoryRepository (신규)
```java
package com.smartclinic.hms.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotHistoryRepository extends JpaRepository<ChatbotHistory, Long> {
    java.util.List<ChatbotHistory> findByStaffIdOrderByCreatedAtDesc(Long staffId);
}
```

### 9. LLM Repository 5개 (신규)
- `DoctorScheduleRepository` — `findByDoctorId`, `findByDoctorIdAndDayOfWeek` 포함
- `MedicalHistoryRepository` — `findBySessionId`, `findByStaffId` 포함
- `MedicalContentRepository`, `MedicalQaRepository`, `MedicalDomainRepository` — 기본 JpaRepository

## 수용 기준
- [ ] `./gradlew build` 오류 없음
- [ ] JPA Entity 스캔 정상 (중복 테이블 매핑 없음)
