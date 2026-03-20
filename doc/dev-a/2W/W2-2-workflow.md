# W2-2 Workflow — 진료과 → 전문의 동적 조회 (AJAX)

> **작성일**: 2W
> **목표**: 진료과 select 변경 시 AJAX로 전문의 목록을 DB에서 조회하여 동적 갱신

---

## 전체 흐름

```
진료과 select 변경 → AJAX → DoctorRepository → JSON 응답 → doctor select 갱신
```

---

## 인터뷰 결과

| 항목 | 내용 |
|------|------|
| department select value | 텍스트("내과") → DB id("1")로 교체 |
| doctor select | 하드코딩 → AJAX 동적 조회 |
| API 엔드포인트 | `GET /api/reservation/doctors?departmentId={id}` |
| 비회원 접근 | SecurityConfig `permitAll` 필요 — config 담당자 요청 |
| 임시 Repository | B 작업자 영역 — A가 임시 생성, 추후 교체 |

---

## 실행 흐름

```
사용자: 진료과 select 변경
    ↓
JS: fetch('GET /api/reservation/doctors?departmentId={id}')
    ↓
ReservationApiController.getDoctors(departmentId)
    ↓
ReservationService.getDoctorsByDepartment(departmentId)
    ↓
DoctorRepository.findByDepartment_Id(departmentId)
    ↓
List<Doctor> → List<DoctorDto> 변환 (id, name)
    ↓
JSON 응답: [{"id":1,"name":"의사이영희"}, ...]
    ↓
JS: doctor select 옵션 교체
```

---

## UI Mockup

```
┌─────────────────────────────────┐
│ 진료과  [내과(1) ▼            ] │  ← value = DB id
│ 전문의  [선택해주세요 ▼       ] │  ← AJAX로 갱신
│         [의사이영희           ] │
│         [의사김민준           ] │
└─────────────────────────────────┘
  ↑ 진료과 변경 시 doctor select 초기화 후 재조회
```

---

## 작업 목록

1. `sql_test.sql` — 진료과 4개 + 의사 여러 명 샘플 데이터 추가
2. `doctor/DoctorRepository.java` — 임시 생성, `findByDepartment_Id` 메서드
3. `doctor/DoctorDto.java` — 임시 생성, 응답 DTO (id, name)
4. `ReservationService.java` — `getDoctorsByDepartment()` 구현
5. `ReservationApiController.java` — `GET /api/reservation/doctors` 엔드포인트
6. `direct-reservation.mustache` — department option value 교체 + JS AJAX 추가

---

## 작업 진행내용

- [x] sql_test.sql 샘플 데이터 추가
- [x] DoctorRepository 임시 생성
- [x] DoctorDto 임시 생성
- [x] ReservationService getDoctorsByDepartment() 구현
- [x] ReservationApiController GET /api/reservation/doctors 추가
- [x] direct-reservation.mustache AJAX 추가

---

## 실행 흐름에 대한 코드

### DoctorRepository (임시)

```java
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByDepartment_Id(Long departmentId);
}
```

### DoctorDto (임시)

```java
@Getter
public class DoctorDto {
    private final Long id;
    private final String name;
    public DoctorDto(Doctor doctor) {
        this.id = doctor.getId();
        this.name = doctor.getStaff().getName();
    }
}
```

### ReservationService

```java
public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
    return doctorRepository.findByDepartment_Id(departmentId)
            .stream().map(DoctorDto::new).toList();
}
```

### ReservationApiController

```java
@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationApiController {
    private final ReservationService reservationService;

    @GetMapping("/doctors")
    public List<DoctorDto> getDoctors(@RequestParam Long departmentId) {
        return reservationService.getDoctorsByDepartment(departmentId);
    }
}
```

### direct-reservation.mustache — JS AJAX

```javascript
// 진료과 변경 시 전문의 동적 조회
document.getElementById('department').addEventListener('change', function () {
    const departmentId = this.value;
    const doctorSelect = document.getElementById('doctor');
    doctorSelect.innerHTML = '<option value="">선택해주세요</option>';
    if (!departmentId) return;
    fetch(`/api/reservation/doctors?departmentId=${departmentId}`)
        .then(res => res.json())
        .then(doctors => {
            doctors.forEach(doctor => {
                const option = document.createElement('option');
                option.value = doctor.id;
                option.textContent = doctor.name;
                doctorSelect.appendChild(option);
            });
        });
});
```

---

## 테스트 진행

| 케이스 | 조건 | 기대 결과 |
|--------|------|-----------|
| 진료과 선택 | departmentId=1 | 해당 과 전문의만 표시 |
| 진료과 변경 | 다른 진료과 선택 | doctor select 초기화 후 재조회 |
| API 직접 호출 | GET /api/reservation/doctors?departmentId=1 | JSON 배열 응답 |
| 비로그인 접근 | 인증 없이 API 호출 | 정상 응답 (permitAll) |

---

## 완료 기준

- [x] 진료과 선택 시 해당 과 전문의만 doctor select에 표시
- [x] 진료과 변경 시 doctor select 초기화 후 재조회
- [x] `GET /api/reservation/doctors?departmentId=1` JSON 배열 응답
- [x] sql_test.sql 진료과 4개 + 의사 5명 샘플 데이터 존재
