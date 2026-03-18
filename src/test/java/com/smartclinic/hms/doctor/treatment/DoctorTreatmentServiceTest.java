package com.smartclinic.hms.doctor.treatment;

import com.smartclinic.hms.doctor.DoctorRepository;
import com.smartclinic.hms.doctor.treatment.dto.DoctorReservationDto;
import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationSource;
import com.smartclinic.hms.domain.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class DoctorTreatmentServiceTest {

    @Mock
    private DoctorReservationRepository reservationRepository;

    @Mock
    private DoctorTreatmentRecordRepository treatmentRecordRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorTreatmentService treatmentService;

    @Test
    @DisplayName("getTodayReceivedList — 오늘 RECEIVED 상태 예약 목록을 반환한다")
    void getTodayReceivedList_returnsReceivedReservations() {
        // given
        String username = "doctor01";
        Reservation reservation = mockReservation("김명준", "09:00", ReservationStatus.RECEIVED, ReservationSource.ONLINE);
        given(reservationRepository.findTodayByDoctorAndStatuses(
                eq(username), any(LocalDate.class), anyList()))
                .willReturn(List.of(reservation));

        // when
        List<DoctorReservationDto> result = treatmentService.getTodayReceivedList(username);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPatientName()).isEqualTo("김명준");
        assertThat(result.get(0).getTimeSlot()).isEqualTo("09:00");
        assertThat(result.get(0).getStatusText()).isEqualTo("진료 대기");
        then(reservationRepository).should()
                .findTodayByDoctorAndStatuses(eq(username), any(LocalDate.class), anyList());
    }

    @Test
    @DisplayName("getTodayReceivedList — RECEIVED 예약이 없으면 빈 리스트를 반환한다")
    void getTodayReceivedList_returnsEmptyListWhenNone() {
        // given
        String username = "doctor01";
        given(reservationRepository.findTodayByDoctorAndStatuses(
                eq(username), any(LocalDate.class), anyList()))
                .willReturn(List.of());

        // when
        List<DoctorReservationDto> result = treatmentService.getTodayReceivedList(username);

        // then
        assertThat(result).isEmpty();
    }

    private Reservation mockReservation(String patientName, String timeSlot,
                                        ReservationStatus status, ReservationSource source) {
        Patient patient = mock(Patient.class);
        given(patient.getName()).willReturn(patientName);
        given(patient.getNote()).willReturn(null);

        Reservation reservation = mock(Reservation.class);
        given(reservation.getPatient()).willReturn(patient);
        given(reservation.getTimeSlot()).willReturn(timeSlot);
        given(reservation.getStatus()).willReturn(status);
        given(reservation.getSource()).willReturn(source);
        return reservation;
    }
}
