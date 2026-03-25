package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.domain.Doctor;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import com.smartclinic.hms.reservation.reservation.PatientRepository;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalkinService {

        private final ReservationRepository reservationRepository;
        private final ReceptionService receptionService;
        private final PatientRepository patientRepository;
        private final DoctorRepository doctorRepository;
        private final DepartmentRepository departmentRepository;
        private final ReservationNumberGenerator reservationNumberGenerator;

        // 방문 접수 생성
        @Transactional
        public boolean createWalkin(WalkinRequestDto request) {

                // 1. 기존 환자 조회 (없으면 신규 생성)
                Patient patient = patientRepository.findByPhone(request.getPhone()).orElse(null);
                boolean nameMismatch = false;

                if (patient == null) {
                        // 신규 환자 생성 시 생년월일/성별 정보 포함하여 저장
                        patient = patientRepository.save(
                                        Patient.create(
                                                        request.getName(),
                                                        request.getPhone(),
                                                        null));
                        patient.updateMedicalInfo(request.getBirthInfo(), request.getVisitReason());
                } else {
                        // 기존 환자라도 이번에 입력한 의료 정보(생년월일, 내원 사유 등)로 최신화
                        if (!patient.getName().equals(request.getName())) {
                                nameMismatch = true;
                        }
                        patient.updateMedicalInfo(request.getBirthInfo(), request.getVisitReason());
                }

                // [주석] 환자의 주소 및 기타 메모 정보를 업데이트합니다.
                String combinedAddress = null;
                if (request.getAddress() != null && !request.getAddress().isBlank()) {
                        combinedAddress = String.format("[%s] %s %s", 
                                request.getZipcode(), request.getAddress(), request.getDetailAddress()).trim();
                }
                patient.updateAddressAndNote(combinedAddress, request.getNotes());

                // [주석] 선택된 의사와 진료과 정보를 조회합니다.
                Doctor doctor = doctorRepository.findById(request.getDoctorId())
                                .orElseThrow(() -> CustomException
                                                .notFound("의사를 찾을 수 없습니다. ID: " + request.getDoctorId()));

                Department department = departmentRepository.findById(request.getDepartmentId())
                                .orElseThrow(() -> CustomException
                                                .notFound("진료과를 찾을 수 없습니다. ID: " + request.getDepartmentId()));

                // 2. 예약/접수 데이터 처리
                if (request.getReservationId() != null) {
                        // [주석] 기존 예약 정보를 가지고 넘어온 경우 (예약 환자 접수)
                        Reservation reservation = reservationRepository.findById(request.getReservationId())
                                        .orElseThrow(() -> CustomException
                                                        .notFound("예약 정보를 찾을 수 없습니다. ID: " + request.getReservationId()));

                        // [주석] 상태를 '진료 대기'로 변경합니다.
                        reservation.receive();
                        
                } else {
                        // [주석] 새로운 방문 접수인 경우 (일반 현장 접수) — 접수 시각을 현재 시간으로 자동 설정
                        LocalDate reservationDate = request.getDate();
                        String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

                        String reservationNumber = reservationNumberGenerator.generate(
                                        reservationDate,
                                        () -> reservationRepository.countByReservationDate(reservationDate));

                        Reservation reservation = Reservation.create(
                                        reservationNumber,
                                        patient,
                                        doctor,
                                        department,
                                        reservationDate,
                                        nowTime,
                                        ReservationSource.WALKIN);

                        // 방문 접수는 생성 즉시 '진료 대기'(RECEIVED) 상태로 변경
                        reservation.receive();

                        reservationRepository.save(reservation);
                }

                return nameMismatch;
        }
}
