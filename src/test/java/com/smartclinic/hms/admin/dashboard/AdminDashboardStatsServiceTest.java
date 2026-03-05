package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminDashboardStatsServiceTest {

    @Mock
    private AdminDashboardStatsRepository adminDashboardStatsRepository;

    @InjectMocks
    private AdminDashboardStatsService adminDashboardStatsService;

    @Test
    @DisplayName("대시보드 통계 4종을 집계해 응답한다")
    void getDashboardStats_returnsAggregatedMetrics() {
        // given
        LocalDate today = LocalDate.of(2026, 3, 5);
        given(adminDashboardStatsRepository.countReservationsByDate(today)).willReturn(5L);
        given(adminDashboardStatsRepository.countAllReservations()).willReturn(120L);
        given(adminDashboardStatsRepository.countActiveStaff()).willReturn(18L);
        given(adminDashboardStatsRepository.countLowStockItems()).willReturn(3L);

        // when
        AdminDashboardStatsResponse response = adminDashboardStatsService.getDashboardStats(today);

        // then
        assertThat(response.todayReservations()).isEqualTo(5L);
        assertThat(response.totalReservations()).isEqualTo(120L);
        assertThat(response.totalStaff()).isEqualTo(18L);
        assertThat(response.lowStockItems()).isEqualTo(3L);

        then(adminDashboardStatsRepository).should().countReservationsByDate(today);
        then(adminDashboardStatsRepository).should().countAllReservations();
        then(adminDashboardStatsRepository).should().countActiveStaff();
        then(adminDashboardStatsRepository).should().countLowStockItems();
    }
}
