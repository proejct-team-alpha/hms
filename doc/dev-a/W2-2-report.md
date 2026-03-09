# W2-2. 진료과 → 전문의 동적 조회 (AJAX) — 작업 리포트

**작업일**: 2026-03-09

---

## 작업 내용

진료과 select 변경 시 AJAX로 해당 과 소속 전문의 목록을 DB에서 조회하여
doctor select를 동적으로 채우도록 구현했다.

---

## 변경/생성 파일

| 파일 | 구분 | 작업 내용 |
|------|------|----------|
| `sql_test.sql` | 수정 | 진료과 4개, staff 9명, 의사 5명으로 샘플 데이터 확장 |
| `doctor/DoctorRepository.java` | 임시 생성 (B 영역) | `findByDepartment_Id` 메서드 |
| `doctor/DoctorDto.java` | 임시 생성 (B 영역) | 응답 DTO (id, name) |
| `reservation/ReservationService.java` | 수정 | `getDoctorsByDepartment()` 구현 |
| `reservation/ReservationApiController.java` | 신규 생성 | `GET /api/reservation/doctors` 엔드포인트 |
| `direct-reservation.mustache` | 수정 | department value id 교체, doctor 옵션 동적화, AJAX JS 추가 |

---

## 상세 변경 사항

### sql_test.sql
- department: 2개 → 4개 (소아과, 이비인후과 추가)
- staff: 5명 → 9명 (doctor02~05 추가)
- doctor: 1명 → 5명 (외과 2명, 소아과 1명, 이비인후과 1명 추가)

### doctor/DoctorRepository.java (임시)
```java
// B 작업자 정식 구현 전까지 임시 생성
List<Doctor> findByDepartment_Id(Long departmentId);
```

### doctor/DoctorDto.java (임시)
```java
// id + staff.name 반환
public DoctorDto(Doctor doctor) {
    this.id = doctor.getId();
    this.name = doctor.getStaff().getName();
}
```

### ReservationService.java
```java
public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
    return doctorRepository.findByDepartment_Id(departmentId)
            .stream().map(DoctorDto::new).toList();
}
```

### ReservationApiController.java
```
GET /api/reservation/doctors?departmentId={id} → List<DoctorDto> JSON
```

### direct-reservation.mustache
| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| department option value | `"내과"`, `"외과"` ... | `"1"`, `"2"` ... (DB id) |
| doctor select | 하드코딩 옵션 3개 | 초기 "선택해주세요"만, AJAX로 동적 교체 |
| JS | AI 추천 로직만 | department change 이벤트 + fetch AJAX 추가 |

---

## 수용 기준 확인

- [x] 진료과 선택 시 해당 과 전문의만 doctor select에 표시
- [x] 진료과 변경 시 doctor select 초기화 후 재조회
- [x] `GET /api/reservation/doctors?departmentId=1` → JSON 배열 응답
- [x] sql_test.sql에 진료과 4개 + 의사 5명 샘플 데이터 존재

---

## 참조 문서

- `doc/dev-a/W2-2-workflow.md`
