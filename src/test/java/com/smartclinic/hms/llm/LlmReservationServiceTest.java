package com.smartclinic.hms.llm;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.domain.*;
import com.smartclinic.hms.llm.dto.LlmReservationResponse;
import com.smartclinic.hms.llm.service.LlmReservationService;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LLM 예약 서비스 단위 테스트")
class LlmReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock DoctorScheduleRepository doctorScheduleRepository;

    @InjectMocks LlmReservationService llmReservationService;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        Department dept = Department.create("내과", true);
        Staff staff = Staff.create("doc1", "D001", "{noop}pw", "김의사", StaffRole.DOCTOR, dept);
        doctor = Doctor.create(staff, dept, "MON,TUE,WED,THU,FRI", "내과");
    }

    @Test
    @DisplayName("존재하지 않는 의사 ID - IllegalArgumentException 발생")
    void getAvailableSlots_의사없음_예외() {
        // given
        given(doctorRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> llmReservationService.getAvailableSlots(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Doctor not found");
    }

    @Test
    @DisplayName("스케줄 없는 의사 - 빈 슬롯 목록 반환")
    void getAvailableSlots_스케줄없음_빈슬롯() {
        // given
        given(doctorRepository.findById(1L)).willReturn(Optional.of(doctor));
        given(doctorScheduleRepository.findByDoctor_IdAndIsAvailableTrue(1L)).willReturn(List.of());
        given(reservationRepository.findBookedSlotsBetween(eq(1L), any(), any())).willReturn(List.of());

        // when
        LlmReservationResponse.SlotList result = llmReservationService.getAvailableSlots(1L);

        // then
        assertThat(result.getDoctorName()).isEqualTo("김의사");
        assertThat(result.getSlots()).isEmpty();
    }

    @Test
    @DisplayName("N+1 방지 - findBookedSlotsBetween 정확히 1번 호출, countByDoctor 미호출")
    void getAvailableSlots_배치조회_단일호출() {
        // given
        given(doctorRepository.findById(1L)).willReturn(Optional.of(doctor));
        given(doctorScheduleRepository.findByDoctor_IdAndIsAvailableTrue(1L)).willReturn(List.of());
        given(reservationRepository.findBookedSlotsBetween(eq(1L), any(), any())).willReturn(List.of());

        // when
        llmReservationService.getAvailableSlots(1L);

        // then - 배치 조회 1회, 개별 count 쿼리 없음
        then(reservationRepository).should(times(1))
                .findBookedSlotsBetween(eq(1L), any(LocalDate.class), any(LocalDate.class));
        then(reservationRepository).should(never())
                .countByDoctor_IdAndReservationDateAndStartTime(any(), any(), any());
    }

    @Test
    @DisplayName("예약된 슬롯은 available=false, 빈 슬롯은 available=true")
    void getAvailableSlots_예약된슬롯_불가표시() {
        // given - 오늘로부터 +1일(내일)이 어떤 요일이든 MON~FRI 스케줄로 커버
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String dayCode = toDayCode(tomorrow);

        DoctorSchedule schedule = new DoctorSchedule(
                doctor, dayCode,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0));  // 09:00~10:00 → 슬롯 2개: 09:00, 09:30

        given(doctorRepository.findById(1L)).willReturn(Optional.of(doctor));
        given(doctorScheduleRepository.findByDoctor_IdAndIsAvailableTrue(1L)).willReturn(List.of(schedule));

        // 09:00 슬롯이 예약됨
        LocalTime bookedSlot = LocalTime.of(9, 0);
        List<Object[]> bookedSlots = new java.util.ArrayList<>();
        bookedSlots.add(new Object[]{tomorrow, bookedSlot});
        given(reservationRepository.findBookedSlotsBetween(eq(1L), any(), any()))
                .willReturn(bookedSlots);

        // when
        LlmReservationResponse.SlotList result = llmReservationService.getAvailableSlots(1L);

        // then
        assertThat(result.getSlots()).hasSizeGreaterThanOrEqualTo(2);
        LlmReservationResponse.Slot slot900 = result.getSlots().stream()
                .filter(s -> s.getStartTime().equals(bookedSlot))
                .findFirst().orElseThrow();
        LlmReservationResponse.Slot slot930 = result.getSlots().stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(9, 30)))
                .findFirst().orElseThrow();

        assertThat(slot900.isAvailable()).isFalse();
        assertThat(slot930.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("슬롯 12개 초과 시 조기 종료 - 최대 12개 반환")
    void getAvailableSlots_최대12개_조기종료() {
        // given - MON~FRI 09:00~15:00 (12슬롯/일) 스케줄을 7일간 제공
        given(doctorRepository.findById(1L)).willReturn(Optional.of(doctor));
        given(reservationRepository.findBookedSlotsBetween(eq(1L), any(), any())).willReturn(List.of());

        // 7일 모두 해당 요일 코드로 스케줄 등록
        List<DoctorSchedule> schedules = List.of(
                new DoctorSchedule(doctor, "MON", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new DoctorSchedule(doctor, "TUE", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new DoctorSchedule(doctor, "WED", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new DoctorSchedule(doctor, "THU", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new DoctorSchedule(doctor, "FRI", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new DoctorSchedule(doctor, "SAT", LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new DoctorSchedule(doctor, "SUN", LocalTime.of(9, 0), LocalTime.of(15, 0))
        );
        given(doctorScheduleRepository.findByDoctor_IdAndIsAvailableTrue(1L)).willReturn(schedules);

        // when
        LlmReservationResponse.SlotList result = llmReservationService.getAvailableSlots(1L);

        // then - 12개 제한 적용
        assertThat(result.getSlots()).hasSizeLessThanOrEqualTo(12);
    }

    private String toDayCode(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "MON";
            case TUESDAY -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY -> "THU";
            case FRIDAY -> "FRI";
            case SATURDAY -> "SAT";
            case SUNDAY -> "SUN";
        };
    }
}
