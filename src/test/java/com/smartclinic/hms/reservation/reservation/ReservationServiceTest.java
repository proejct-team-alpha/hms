package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * 예약 서비스 테스트
 * 프로젝트 규칙(rule_test.md §3.6.4)에 따라 클래스 레벨 @DisplayName 추가
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("비회원 예약 서비스 테스트")
class ReservationServiceTest {

    @Mock DoctorRepository doctorRepository;
    @Mock PatientRepository patientRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock DepartmentRepository departmentRepository;

    @InjectMocks ReservationService reservationService;

    private ReservationCreateForm form;
    private Doctor doctor;
    private Department department;
    private Patient patient;

    @BeforeEach
    void setUp() {
        // ReservationCreateForm이 record로 변경됨에 따라 생성자를 통해 초기화
        form = new ReservationCreateForm(
            "홍길동",
            "01012345678",
            1L,
            1L,
            LocalDate.of(2026, 4, 1),
            "09:00"
        );

        department = Department.create("내과", true);
        Staff staff = Staff.create("doctor1", "D001", "{noop}pw", "김내과", StaffRole.DOCTOR, department);
        doctor = Doctor.create(staff, department, "MON,TUE,WED,THU,FRI", "내과");
        patient = Patient.create("홍길동", "01012345678", null);
    }

    @Test
    @DisplayName("예약 생성 성공 - 기존 환자 재사용, 예약번호 RES- 형식")
    void createReservation_success_existingPatient() {
        // given
        given(reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                anyLong(), any(), anyString(), any())).willReturn(false);
        given(patientRepository.findByPhone("01012345678")).willReturn(Optional.of(patient));
        given(doctorRepository.findById(1L)).willReturn(Optional.of(doctor));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(reservationRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);

        // when
        ReservationCompleteInfo info = reservationService.createReservation(form);

        // then
        assertThat(info.getPatientName()).isEqualTo("홍길동");
        assertThat(info.getDepartmentName()).isEqualTo("내과");
        assertThat(info.getDoctorName()).isEqualTo("김내과");
        assertThat(info.getReservationDate()).isEqualTo("2026-04-01");
        assertThat(info.getTimeSlot()).isEqualTo("09:00");
        assertThat(info.getReservationNumber()).startsWith("RES-");

        // 기존 환자 재사용 - patientRepository.save() 호출 안 됨
        then(patientRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("예약 생성 성공 - 신규 환자 생성")
    void createReservation_success_newPatient() {
        // given
        given(reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                anyLong(), any(), anyString(), any())).willReturn(false);
        given(patientRepository.findByPhone("01012345678")).willReturn(Optional.empty());
        given(patientRepository.save(any(Patient.class))).willReturn(patient);
        given(doctorRepository.findById(1L)).willReturn(Optional.of(doctor));
        given(departmentRepository.findById(1L)).willReturn(Optional.of(department));
        given(reservationRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);

        // when
        ReservationCompleteInfo info = reservationService.createReservation(form);

        // then
        assertThat(info.getPatientName()).isEqualTo("홍길동");
        then(patientRepository).should().save(any(Patient.class));
    }

    @Test
    @DisplayName("중복 예약 시 IllegalStateException 발생")
    void createReservation_duplicateSlot_throwsException() {
        // given
        given(reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                anyLong(), any(), anyString(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(form))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 예약된 시간대입니다.");
    }
}
