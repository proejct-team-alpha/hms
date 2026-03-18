package com.smartclinic.hms.nurse;

import com.smartclinic.hms.domain.Patient;
import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.nurse.dto.NurseDashboardDto;
import com.smartclinic.hms.nurse.dto.NurseStatusFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class NurseServiceTest {

    @Mock
    private NurseReservationRepository reservationRepository;
    @Mock
    private NursePatientRepository patientRepository;

    @InjectMocks
    private NurseService nurseService;

    // ── getDashboard ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getDashboard — 오늘 예약이 없으면 모든 카운트가 0인 대시보드 반환")
    void getDashboard_withNoReservations_returnsZeroCounts() {
        given(reservationRepository.findTodayNonCancelled(any(), any())).willReturn(List.of());

        NurseDashboardDto dto = nurseService.getDashboard();

        assertThat(dto.getTotalToday()).isEqualTo(0);
        assertThat(dto.getWaitingCount()).isEqualTo(0);
        assertThat(dto.getSpecialCount()).isEqualTo(0);
        assertThat(dto.getWaitingList()).isEmpty();
    }

    // ── getStatusFilters ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getStatusFilters — null 입력 시 전체 필터가 선택됨")
    void getStatusFilters_withNull_selectsAllFilter() {
        List<NurseStatusFilter> filters = nurseService.getStatusFilters(null);

        assertThat(filters).hasSize(4);
        assertThat(filters.get(0).isSelected()).isTrue();
        assertThat(filters.get(1).isSelected()).isFalse();
    }

    @Test
    @DisplayName("getStatusFilters — RECEIVED 입력 시 해당 필터가 선택됨")
    void getStatusFilters_withReceived_selectsReceivedFilter() {
        List<NurseStatusFilter> filters = nurseService.getStatusFilters("RECEIVED");

        assertThat(filters.get(0).isSelected()).isFalse();
        NurseStatusFilter receivedFilter = filters.stream()
                .filter(f -> "RECEIVED".equals(f.getValue()))
                .findFirst().orElseThrow();
        assertThat(receivedFilter.isSelected()).isTrue();
    }

    // ── getReceptionPage ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getReceptionPage — 잘못된 상태 입력 시 전체 목록으로 대체 조회")
    void getReceptionPage_withInvalidStatus_fallsBackToAll() {
        Page<Reservation> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(reservationRepository.findTodayNonCancelledPage(any(), any(), any()))
                .willReturn(emptyPage);

        nurseService.getReceptionPage("INVALID_STATUS", 0);

        then(reservationRepository).should()
                .findTodayNonCancelledPage(any(), eq(ReservationStatus.CANCELLED), any());
    }

    @Test
    @DisplayName("getReceptionPage — 유효한 상태 입력 시 해당 상태로 조회")
    void getReceptionPage_withValidStatus_queriesByStatus() {
        Page<Reservation> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        given(reservationRepository.findTodayByStatusPage(any(), any(), any()))
                .willReturn(emptyPage);

        nurseService.getReceptionPage("RECEIVED", 0);

        then(reservationRepository).should()
                .findTodayByStatusPage(any(), eq(ReservationStatus.RECEIVED), any());
    }

    // ── receiveReservation ───────────────────────────────────────────────────

    @Test
    @DisplayName("receiveReservation — 예약에 대해 receive() 호출")
    void receiveReservation_callsReceiveOnReservation() {
        Reservation r = mock(Reservation.class);
        given(reservationRepository.findById(1L)).willReturn(Optional.of(r));

        nurseService.receiveReservation(1L);

        then(r).should().receive();
    }

    // ── updatePatient ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updatePatient — 환자에 대해 updateInfo() 호출")
    void updatePatient_callsUpdateInfoOnPatient() {
        Patient patient = mock(Patient.class);
        given(patientRepository.findById(1L)).willReturn(Optional.of(patient));

        nurseService.updatePatient(1L, "010-1234-5678", "서울시 강남구", "특이사항 없음");

        then(patient).should()
                .updateInfo(any(), eq("010-1234-5678"), any(), eq("서울시 강남구"), eq("특이사항 없음"));
    }
}
