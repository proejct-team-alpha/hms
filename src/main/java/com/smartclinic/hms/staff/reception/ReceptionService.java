package com.smartclinic.hms.staff.reception;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.PatientRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.reservation.reservation.ReservationService;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
import com.smartclinic.hms.staff.reservation.dto.PhoneReservationRequestDto;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ReceptionService {

    private final ReservationRepository reservationRepository;
    private final DoctorRepository doctorRepository;
    private final DepartmentRepository departmentRepository;
    private final PatientRepository patientRepository;
    private final ReservationService reservationService;

    @Transactional
    public void receive(ReceptionUpdateRequest request) {

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new RuntimeException("예약 없음"));

        reservation.receive();
    }

    @Transactional
    public List<Reservation> getReservations() {

        List<Reservation> reservations = reservationRepository.findAll();

        // LAZY 강제 로딩
        for (Reservation r : reservations) {
            r.getPatient().getName();
            r.getDoctor().getStaff().getName();
            r.getDepartment().getName();
        }

        return reservations;
    }

    @Transactional
    public void createPhoneReservation(PhoneReservationRequestDto request) {

        String phone = request.getPhone();

        // 1. 환자 조회
        Patient patient = patientRepository.findByPhone(phone).orElse(null);

        // 2. 환자 없으면 신규 생성
        if (patient == null) {
            patient = Patient.create(
                    request.getName(),
                    phone,
                    request.getEmail());

            patientRepository.save(patient);
        }

        // 3. 의사 조회
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("의사 없음"));

        // 4. 진료과 조회
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("진료과 없음"));

        // 5. 예약번호 생성
        String reservationNumber = reservationService.generateReservationNumber();

        // 6. 예약 생성
        Reservation reservation = Reservation.create(
                reservationNumber,
                patient,
                doctor,
                department,
                LocalDate.parse(request.getDate()),
                request.getTime(),
                ReservationSource.PHONE);

        reservationRepository.save(reservation);
    }

}
