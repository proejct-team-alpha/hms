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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 데이터베이스에서 특정 진료과(`departmentId`)에 소속된 의사 목록을 조회하는 인터페이스입니다. `JpaRepository`를 상속받아 기본 CRUD 기능도 자동으로 제공됩니다.
> - **왜 이렇게 썼는지**: Spring Data JPA는 `findBy필드명` 형태의 메서드 이름만 만들면 쿼리를 자동 생성해 줍니다. `Department_Id`는 "Doctor 엔티티의 department 필드 안에 있는 id"를 의미합니다.
> - **쉽게 말하면**: "내과 소속 의사 모두 찾아줘"라고 말하면 DB가 알아서 찾아주는 자동 검색 서비스입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: DB에서 조회한 `Doctor` 엔티티에서 필요한 정보(id, 이름)만 골라 담는 데이터 전달 객체(DTO)입니다. 이 객체가 JSON으로 변환되어 브라우저로 전송됩니다.
> - **왜 이렇게 썼는지**: 엔티티 전체를 외부에 노출하면 불필요한 정보가 새어나가고 보안 문제가 생길 수 있습니다. DTO는 필요한 필드만 선택적으로 담아 응답하는 용도입니다. `@Getter`는 Lombok 라이브러리 어노테이션으로 자동으로 `getId()`, `getName()` 메서드를 만들어줍니다.
> - **쉽게 말하면**: 의사 정보가 가득 담긴 파일에서 필요한 부분(id, 이름)만 복사해서 전달하는 요약본입니다.

### ReservationService

```java
public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
    return doctorRepository.findByDepartment_Id(departmentId)
            .stream().map(DoctorDto::new).toList();
}
```

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 진료과 ID를 받아 해당 진료과 소속 의사 목록을 DB에서 조회한 뒤, 각 `Doctor` 객체를 `DoctorDto`로 변환하여 리스트로 반환합니다.
> - **왜 이렇게 썼는지**: `stream().map(DoctorDto::new).toList()`는 Java 스트림 API로, 리스트의 각 원소를 변환하는 방법입니다. `DoctorDto::new`는 "각 Doctor를 DoctorDto 생성자로 변환해라"는 의미입니다.
> - **쉽게 말하면**: 창고(DB)에서 내과 의사 목록을 꺼내서, 각각 요약 카드(DoctorDto)로 바꿔 리스트로 묶어 전달하는 과정입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: `/api/reservation/doctors?departmentId=1` 형태의 API 요청이 오면, 해당 진료과 의사 목록을 JSON 형태로 응답합니다.
> - **왜 이렇게 썼는지**: `@RestController`는 화면 대신 데이터(JSON)를 직접 반환하는 컨트롤러입니다. `@RequestParam`은 URL의 `?departmentId=1` 부분에서 값을 읽어옵니다. `@RequiredArgsConstructor`는 Lombok이 `final` 필드의 생성자를 자동으로 만들어주는 어노테이션입니다.
> - **쉽게 말하면**: "내과 의사 목록 주세요"라는 AJAX 요청에 "[{id:1, name:'이영희'}, ...]" 형태의 JSON 데이터로 답변해주는 API 창구입니다.

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

> **💡 입문자 설명**
> - **이 코드가 하는 일**: 사용자가 진료과 선택 드롭다운을 바꿀 때마다 서버에 AJAX 요청을 보내 해당 진료과 의사 목록을 받아 전문의 드롭다운을 동적으로 갱신합니다.
> - **왜 이렇게 썼는지**: 페이지를 새로 고침하지 않고 데이터를 가져오려면 AJAX(`fetch`)를 사용합니다. `addEventListener('change', ...)`는 드롭다운 값이 바뀔 때 함수를 실행하는 이벤트 리스너입니다. `.then(res => res.json())`은 서버 응답을 JSON으로 파싱합니다.
> - **쉽게 말하면**: 진료과를 바꾸면 서버에 "이 과 의사 목록 주세요"라고 몰래 요청해서, 페이지 전환 없이 의사 드롭다운만 바꿔치기하는 방식입니다.

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
