package com.smartclinic.hms.reservation.reservation;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.ReservationNumberGenerator;
import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
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
    @Mock ReservationNumberGenerator reservationNumberGenerator;

    @InjectMocks ReservationService reservationService;

    private CreateReservationRequest form;
    private Doctor doctor;
    private Department department;
    private Patient patient;

    @BeforeEach
    void setUp() {
        form = new CreateReservationRequest(
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
        given(reservationNumberGenerator.generate(any(LocalDate.class), any())).willReturn("RES-20260401-001");

        // when
        ReservationCompleteInfo info = reservationService.createReservation(form);

        // then
        assertThat(info.patientName()).isEqualTo("홍길동");
        assertThat(info.departmentName()).isEqualTo("내과");
        assertThat(info.doctorName()).isEqualTo("김내과");
        assertThat(info.reservationDate()).isEqualTo("2026-04-01");
        assertThat(info.timeSlot()).isEqualTo("09:00");
        assertThat(info.reservationNumber()).startsWith("RES-");

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
        given(reservationNumberGenerator.generate(any(LocalDate.class), any())).willReturn("RES-20260401-001");

        // when
        ReservationCompleteInfo info = reservationService.createReservation(form);

        // then
        assertThat(info.patientName()).isEqualTo("홍길동");
        then(patientRepository).should().save(any(Patient.class));
    }

    @Test
    @DisplayName("예약된 슬롯 조회 - 직접 예약용")
    void getBookedTimeSlots_returnsBookedSlots() {
        // given
        given(reservationRepository.findBookedTimeSlots(
                1L, LocalDate.of(2026, 4, 1), ReservationStatus.CANCELLED))
            .willReturn(List.of("09:00", "10:30"));

        // when
        List<String> slots = reservationService.getBookedTimeSlots(1L, LocalDate.of(2026, 4, 1));

        // then
        assertThat(slots).containsExactlyInAnyOrder("09:00", "10:30");
    }

    @Test
    @DisplayName("예약된 슬롯 조회 - 현재 예약 제외 (변경용)")
    void getBookedTimeSlots_withExcludeId_excludesCurrentReservation() {
        // given
        given(reservationRepository.findBookedTimeSlotsExcluding(
                1L, LocalDate.of(2026, 4, 1), ReservationStatus.CANCELLED, 5L))
            .willReturn(List.of("10:30"));

        // when
        List<String> slots = reservationService.getBookedTimeSlots(1L, LocalDate.of(2026, 4, 1), 5L);

        // then
        assertThat(slots).containsExactly("10:30");
    }

    @Test
    @DisplayName("중복 예약 시 IllegalStateException 발생")
    void createReservation_duplicateSlot_throwsException() {
        // given
        given(reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                anyLong(), any(), anyString(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(form))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("이미 예약된 시간대입니다.");
    }

    @Test
    @DisplayName("예약 취소 성공 - 전화번호 일치 시 ReservationCompleteInfo 반환 및 cancelFully 호출")
    void cancelReservation_success_returnsCompleteInfo() {
        // given - Mockito.mock으로 Reservation 엔티티 연관관계 스텁 처리
        Reservation reservation = Mockito.mock(Reservation.class);
        given(reservation.getReservationNumber()).willReturn("RES-20260401-001");
        given(reservation.getPatient()).willReturn(patient);  // phone: 01012345678
        given(reservation.getDepartment()).willReturn(department);  // name: 내과
        given(reservation.getDoctor()).willReturn(doctor);  // staff name: 김내과
        given(reservation.getReservationDate()).willReturn(LocalDate.of(2026, 4, 1));
        given(reservation.getTimeSlot()).willReturn("09:00");
        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when
        ReservationCompleteInfo info = reservationService.cancelReservation(1L, "01012345678");

        // then
        assertThat(info.reservationNumber()).isEqualTo("RES-20260401-001");
        assertThat(info.patientName()).isEqualTo("홍길동");
        assertThat(info.departmentName()).isEqualTo("내과");
        // cancelFully 호출 확인 → 예약 상태가 CANCELLED로 변경됨
        then(reservation).should().cancelFully(null);
    }

    @Test
    @DisplayName("예약 취소 실패 - 전화번호 불일치 시 403 Forbidden CustomException 발생")
    void cancelReservation_wrongPhone_throwsForbidden() {
        // given - 저장된 전화번호(01012345678)와 다른 번호(01099999999)로 취소 시도
        Reservation reservation = Mockito.mock(Reservation.class);
        given(reservation.getPatient()).willReturn(patient);  // phone: 01012345678
        given(reservationRepository.findById(1L)).willReturn(Optional.of(reservation));

        // when & then
        assertThatThrownBy(() -> reservationService.cancelReservation(1L, "01099999999"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("예약 소유자가 아닙니다.");
    }

    @Test
    @DisplayName("예약 변경 성공 - 기존 예약 취소 후 새 예약 생성, ReservationCompleteInfo 반환")
    void updateReservation_success_cancelsOldAndCreatesNew() {
        // given
        Reservation oldReservation = Mockito.mock(Reservation.class);
        given(oldReservation.getPatient()).willReturn(patient);  // phone: 01012345678
        given(reservationRepository.findByIdForUpdate(1L)).willReturn(Optional.of(oldReservation));
        // 새 슬롯 중복 없음
        given(reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                anyLong(), any(), anyString(), any())).willReturn(false);
        given(doctorRepository.findById(2L)).willReturn(Optional.of(doctor));
        given(departmentRepository.findById(2L)).willReturn(Optional.of(department));
        given(reservationNumberGenerator.generate(any(LocalDate.class), any())).willReturn("RES-20260402-001");

        UpdateReservationRequest updateForm = new UpdateReservationRequest(
                2L, 2L, LocalDate.of(2026, 4, 2), "10:00");

        // when
        ReservationCompleteInfo info = reservationService.updateReservation(1L, "01012345678", updateForm);

        // then
        assertThat(info.reservationNumber()).isEqualTo("RES-20260402-001");
        assertThat(info.timeSlot()).isEqualTo("10:00");
        // 기존 예약 취소 처리 확인
        then(oldReservation).should().cancelFully(null);
        // 새 예약 저장 확인
        then(reservationRepository).should().save(any(Reservation.class));
    }

    @Test
    @DisplayName("예약 변경 실패 - 전화번호 불일치 시 403 Forbidden CustomException 발생")
    void updateReservation_wrongPhone_throwsForbidden() {
        // given - 저장된 전화번호(01012345678)와 다른 번호로 변경 시도
        Reservation oldReservation = Mockito.mock(Reservation.class);
        given(oldReservation.getPatient()).willReturn(patient);  // phone: 01012345678
        given(reservationRepository.findByIdForUpdate(1L)).willReturn(Optional.of(oldReservation));

        UpdateReservationRequest updateForm = new UpdateReservationRequest(
                1L, 1L, LocalDate.of(2026, 4, 1), "09:00");

        // when & then
        assertThatThrownBy(() -> reservationService.updateReservation(1L, "01099999999", updateForm))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("예약 소유자가 아닙니다.");
    }

    @Test
    @DisplayName("예약 변경 실패 - 새 시간대 중복 시 409 Conflict CustomException 발생")
    void updateReservation_duplicateSlot_throwsConflict() {
        // given - 소유권은 통과, 새 슬롯 중복 발생
        Reservation oldReservation = Mockito.mock(Reservation.class);
        given(oldReservation.getPatient()).willReturn(patient);
        given(reservationRepository.findByIdForUpdate(1L)).willReturn(Optional.of(oldReservation));
        given(reservationRepository.existsByDoctor_IdAndReservationDateAndTimeSlotAndStatusNot(
                anyLong(), any(), anyString(), any())).willReturn(true);

        UpdateReservationRequest updateForm = new UpdateReservationRequest(
                1L, 1L, LocalDate.of(2026, 4, 1), "09:00");

        // when & then
        assertThatThrownBy(() -> reservationService.updateReservation(1L, "01012345678", updateForm))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("이미 예약된 시간대입니다.");
    }

    @Test
    @DisplayName("예약번호로 단건 조회 - 존재하는 경우 ReservationInfoDto 반환")
    void findByReservationNumber_exists_returnsDto() {
        // given - ReservationInfoDto 생성에 필요한 모든 필드 스텁 처리
        Reservation reservation = Mockito.mock(Reservation.class);
        given(reservation.getId()).willReturn(1L);
        given(reservation.getReservationNumber()).willReturn("RES-20260401-001");
        given(reservation.getPatient()).willReturn(patient);  // id/name/phone
        given(reservation.getDepartment()).willReturn(department);
        given(reservation.getDoctor()).willReturn(doctor);
        given(reservation.getReservationDate()).willReturn(LocalDate.of(2026, 4, 1));
        given(reservation.getTimeSlot()).willReturn("09:00");
        given(reservation.getStatus()).willReturn(ReservationStatus.RESERVED);
        given(reservationRepository.findByReservationNumber("RES-20260401-001"))
                .willReturn(Optional.of(reservation));

        // when
        Optional<ReservationInfoDto> result =
                reservationService.findByReservationNumber("RES-20260401-001");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getReservationNumber()).isEqualTo("RES-20260401-001");
        assertThat(result.get().getPatientName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("이름 + 전화번호 조회 - 전화번호 정규화 후 ReservationInfoDto 리스트 반환")
    void findByPhoneAndName_normalizedPhone_returnsDto() {
        // given - 하이픈 포함 전화번호 입력 → 숫자만 추출하여 조회
        Reservation reservation = Mockito.mock(Reservation.class);
        given(reservation.getId()).willReturn(1L);
        given(reservation.getReservationNumber()).willReturn("RES-20260401-001");
        given(reservation.getPatient()).willReturn(patient);
        given(reservation.getDepartment()).willReturn(department);
        given(reservation.getDoctor()).willReturn(doctor);
        given(reservation.getReservationDate()).willReturn(LocalDate.of(2026, 4, 1));
        given(reservation.getTimeSlot()).willReturn("09:00");
        given(reservation.getStatus()).willReturn(ReservationStatus.RESERVED);
        // 정규화된 전화번호(하이픈 제거)와 이름으로 조회됨
        given(reservationRepository.findByNormalizedPhoneAndName("01012345678", "홍길동"))
                .willReturn(List.of(reservation));

        // when - 하이픈 포함 전화번호와 공백 포함 이름으로 서비스 호출
        List<ReservationInfoDto> result =
                reservationService.findByPhoneAndName("010-1234-5678", " 홍길동 ");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientName()).isEqualTo("홍길동");
    }
}
