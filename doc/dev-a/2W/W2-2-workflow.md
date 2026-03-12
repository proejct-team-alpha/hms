# W2-2. 진료과 → 전문의 동적 조회 (AJAX)

## 개요

진료과 select 변경 시 AJAX로 해당 과 전문의 목록을 DB에서 조회하여
doctor select를 동적으로 채운다.

현재 `direct-reservation.mustache`의 doctor select는 하드코딩된 옵션이며,
department select의 value도 텍스트("내과")로 되어 있어 DB id와 연결이 안 된 상태다.

---

## 현재 문제점

| 항목 | 현재 (문제) | 변경 후 |
|------|------------|---------|
| department select value | `"내과"`, `"외과"` (텍스트) | `"1"`, `"2"` (DB id) |
| doctor select | 하드코딩 옵션 3개 | AJAX로 동적 조회 |
| DoctorRepository | 없음 | 생성 필요 |
| API 엔드포인트 | 없음 | `GET /api/reservation/doctors?departmentId={id}` |
| Spring Security | `/api/reservation/**` 인증 필요 | **비회원 접근 허용** (`permitAll`) — config 담당자 요청 필요 |
| 샘플 DB | 진료과 2개, 의사 1명 | 진료과 4개, 의사 여러 명 |

---

## 실행 흐름

```
사용자: 진료과 select 변경
    ↓
JS: fetch('GET /api/reservation/doctors?departmentId={선택된 id}')
    ↓
ReservationApiController.getDoctors(departmentId)
    ↓
ReservationService.getDoctorsByDepartment(departmentId)
    ↓
DoctorRepository.findByDepartment_Id(departmentId)  ← JPA
    ↓
List<Doctor> → List<DoctorDto> 변환 (id, staff.name)
    ↓
JSON 응답: [{"id":1,"name":"의사이영희"}, ...]
    ↓
JS: doctor select 옵션 교체
```

---

## 파일 소유권 및 협업 요청

| 파일 | 소유자 | 비고 |
|------|--------|------|
| `doctor/DoctorRepository.java` | **B 작업자** | 임시로 A가 생성, B 작업자가 정식 구현 시 교체 |
| `doctor/DoctorDto.java` | **B 작업자** | 임시로 A가 생성, B 작업자가 정식 구현 시 교체 |
| `reservation/ReservationService.java` | A 작업자 | doctor 패키지의 Repository/Dto를 import해서 사용 |
| `reservation/ReservationApiController.java` | A 작업자 | |
| `reservation/direct-reservation.mustache` | A 작업자 | |
| `config/SecurityConfig.java` | **config 담당자** | ⚠️ A 작업자 접근 불가 — 아래 요청 내용 참고 |

### ⚠️ config 담당자에게 요청

`GET /api/reservation/doctors` 엔드포인트는 비회원 예약 폼에서 호출된다.
로그인 없이 접근 가능하도록 SecurityConfig에 `permitAll` 설정이 필요하다.

```java
// SecurityConfig.java 에 추가 요청
.requestMatchers("/api/reservation/**").permitAll()
```

## 작업 목록

| 순서 | 파일 | 작업 내용 |
|------|------|----------|
| 1 | `sql_test.sql` | 진료과 4개 + 의사 여러 명 샘플 데이터 추가 |
| 2 | `doctor/DoctorRepository.java` | **임시 생성** (B 작업자 영역), `findByDepartment_Id` 메서드 |
| 3 | `doctor/DoctorDto.java` | **임시 생성** (B 작업자 영역), 응답 DTO (id, name) |
| 4 | `reservation/ReservationService.java` | `getDoctorsByDepartment()` 구현, doctor 패키지 import |
| 5 | `reservation/ReservationApiController.java` | `GET /api/reservation/doctors` 엔드포인트 생성 |
| 6 | `direct-reservation.mustache` | department option value 텍스트 → id 교체, JS AJAX 추가 |

---

## 실행 흐름에 대한 코드

### 1. sql_test.sql — 샘플 데이터 추가

```sql
-- 진료과 추가
INSERT INTO department (id, name, is_active) VALUES
(3, '소아과', true),
(4, '이비인후과', true);

-- 직원 추가 (doctor 역할)
INSERT INTO staff (id, username, employee_number, password, name, role, department_id, is_active, created_at) VALUES
(6,  'doctor02', 'D-20260102', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사김민준', 'DOCTOR', 2, true, CURRENT_TIMESTAMP),
(7,  'doctor03', 'D-20260103', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사박서연', 'DOCTOR', 2, true, CURRENT_TIMESTAMP),
(8,  'doctor04', 'D-20260104', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사최지우', 'DOCTOR', 3, true, CURRENT_TIMESTAMP),
(9,  'doctor05', 'D-20260105', '$2a$10$uoi02aP/J54ldleNvQviZuWOHEDvwa5M9RwdBkraLPYljZqBATvpa', '의사이준혁', 'DOCTOR', 4, true, CURRENT_TIMESTAMP);

-- 의사 추가
INSERT INTO doctor (id, staff_id, department_id, available_days, specialty) VALUES
(2, 6, 2, 'MON,WED,FRI', '외상외과'),
(3, 7, 2, 'TUE,THU',     '복강경외과'),
(4, 8, 3, 'MON,TUE,WED,THU,FRI', '소아일반'),
(5, 9, 4, 'MON,TUE,WED,THU,FRI', '이비인후일반');
```

### 2. doctor/DoctorRepository.java — 임시 생성 (B 작업자 영역)

> ⚠️ B 작업자가 정식 구현하기 전까지 A가 임시로 생성. 추후 B가 교체.

```java
package com.smartclinic.hms.doctor;

import com.smartclinic.hms.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByDepartment_Id(Long departmentId);
}
```

### 3. doctor/DoctorDto.java — 임시 생성 (B 작업자 영역)

> ⚠️ B 작업자가 정식 구현하기 전까지 A가 임시로 생성. 추후 B가 교체.

```java
package com.smartclinic.hms.doctor;

import com.smartclinic.hms.domain.Doctor;
import lombok.Getter;

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

### 4. ReservationService.java — getDoctorsByDepartment 추가

```java
import com.smartclinic.hms.doctor.DoctorDto;
import com.smartclinic.hms.doctor.DoctorRepository;

public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
    return doctorRepository.findByDepartment_Id(departmentId)
            .stream()
            .map(DoctorDto::new)
            .toList();
}
```

### 5. ReservationApiController.java

```java
package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.doctor.DoctorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

### 6. direct-reservation.mustache — department value + JS AJAX

**department select option value 교체**
```html
<select id="department" name="departmentId" required ...>
  <option value="">선택해주세요</option>
  <option value="1">내과</option>
  <option value="2">외과</option>
  <option value="3">소아과</option>
  <option value="4">이비인후과</option>
</select>
```

**JS AJAX 추가 (기존 AI 추천 로직 아래에 추가)**
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

## 수용 기준

- [ ] 진료과 선택 시 해당 과 전문의만 doctor select에 표시
- [ ] 진료과 변경 시 doctor select 초기화 후 재조회
- [ ] `GET /api/reservation/doctors?departmentId=1` → JSON 배열 응답
- [ ] sql_test.sql에 진료과 4개 + 의사 5명 샘플 데이터 존재
- [ ] **비로그인 상태에서** `/api/reservation/doctors` 호출 시 정상 응답 (config 담당자 처리 필요)
