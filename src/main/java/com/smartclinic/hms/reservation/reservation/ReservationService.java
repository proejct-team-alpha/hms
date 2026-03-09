package com.smartclinic.hms.reservation.reservation;

// [W2-#4 작업 목록]
// DONE 1. PatientRepository, ReservationRepository, DepartmentRepository 필드 추가
// DONE 2. createReservation() 구현 (Patient 조회/생성 → Doctor/Department 조회 → Reservation 저장)

import com.smartclinic.hms.doctor.DoctorDto;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ReservationRepository reservationRepository;
    private final DepartmentRepository departmentRepository;

    public List<DoctorDto> getDoctorsByDepartment(Long departmentId) {
        return doctorRepository.findByDepartment_Id(departmentId)
                .stream()
                .map(DoctorDto::new)
                .toList();
    }

    @Transactional
    public ReservationCompleteInfo createReservation(ReservationCreateForm form) {
        // 1. 전화번호로 Patient 조회, 없으면 신규 생성
        Patient patient = patientRepository.findByPhone(form.getPhone())
                .orElseGet(() -> patientRepository.save(
                        Patient.create(form.getName(), form.getPhone(), null)));

        // 2. Doctor, Department 조회
        Doctor doctor = doctorRepository.findById(form.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("의사를 찾을 수 없습니다."));
        Department department = departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("진료과를 찾을 수 없습니다."));

        // 3. 예약번호 생성 (R + 14자리 타임스탬프)
        String reservationNumber = "R" + System.currentTimeMillis();

        // 4. Reservation 생성 및 저장
        Reservation reservation = Reservation.create(
                reservationNumber, patient, doctor, department,
                form.getReservationDate(), form.getTimeSlot(),
                ReservationSource.ONLINE
        );
        reservationRepository.save(reservation);

        // 5. 트랜잭션 내에서 LAZY 필드 접근 후 DTO 반환 (LazyInitializationException 방지)
        return new ReservationCompleteInfo(
                patient.getName(),
                department.getName(),
                doctor.getStaff().getName(),
                form.getReservationDate().toString(),
                form.getTimeSlot()
        );
    }
}
