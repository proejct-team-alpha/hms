package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.domain.ItemCategory;
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
    private AdminReservationRepository adminReservationRepository;

    @Mock
    private AdminStaffRepository adminStaffRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private AdminDashboardStatsService adminDashboardStatsService;

    @Test
    @DisplayName("대시보드 통계 4종을 집계해 응답한다")
    void getDashboardStats_returnsAggregatedMetrics() {
        // given
        LocalDate today = LocalDate.of(2026, 3, 5);
        given(adminReservationRepository.countByReservationDate(today)).willReturn(5L);
        given(adminReservationRepository.count()).willReturn(120L);
        given(adminStaffRepository.countByActiveTrue()).willReturn(18L);
        given(itemRepository.countLowStockItems()).willReturn(1L);

        // when
        AdminDashboardStatsResponse response = adminDashboardStatsService.getDashboardStats(today);

        // then
        assertThat(response.todayReservations()).isEqualTo(5L);
        assertThat(response.totalReservations()).isEqualTo(120L);
        assertThat(response.totalStaff()).isEqualTo(18L);
        assertThat(response.lowStockItems()).isEqualTo(1L);

        then(adminReservationRepository).should().countByReservationDate(today);
        then(adminReservationRepository).should().count();
        then(adminStaffRepository).should().countByActiveTrue();
        then(itemRepository).should().countLowStockItems();
    }

    @Test
    @DisplayName("차트 응답 조립 시 7일 범위 누락 날짜를 0으로 보정한다")
    void getDashboardChart_fillsMissingDatesWithZero() {
        // given
        LocalDate today = LocalDate.of(2026, 3, 5);
        LocalDate startDate = today.minusDays(3);
        LocalDate endDate = today.plusDays(3);
        given(itemRepository.findCategoryCounts()).willReturn(java.util.List.of(
                categoryCount(ItemCategory.MEDICAL_SUPPLIES, 5L),
                categoryCount(ItemCategory.MEDICAL_EQUIPMENT, 2L)));
        given(adminReservationRepository.findDailyPatientCounts(startDate, endDate)).willReturn(java.util.List.of(
                dailyCount(today.minusDays(2), 3L),
                dailyCount(today, 4L),
                dailyCount(today.plusDays(1), 1L)));

        // when
        AdminDashboardChartResponse response = adminDashboardStatsService.getDashboardChart(today);

        // then
        assertThat(response.categoryCounts()).hasSize(2);
        assertThat(response.categoryCounts().get(0).totalCount()).isEqualTo(5L);
        assertThat(response.dailyPatients()).hasSize(7);
        assertThat(response.dailyPatients().get(0).date()).isEqualTo(today.minusDays(3));
        assertThat(response.dailyPatients().get(0).patientCount()).isEqualTo(0L);
        assertThat(response.dailyPatients().get(1).date()).isEqualTo(today.minusDays(2));
        assertThat(response.dailyPatients().get(1).patientCount()).isEqualTo(3L);
        assertThat(response.dailyPatients().get(3).date()).isEqualTo(today);
        assertThat(response.dailyPatients().get(3).patientCount()).isEqualTo(4L);
        assertThat(response.dailyPatients().get(4).date()).isEqualTo(today.plusDays(1));
        assertThat(response.dailyPatients().get(4).patientCount()).isEqualTo(1L);
        assertThat(response.dailyPatients().get(6).date()).isEqualTo(today.plusDays(3));
        assertThat(response.dailyPatients().get(6).patientCount()).isEqualTo(0L);

        then(itemRepository).should().findCategoryCounts();
        then(adminReservationRepository).should().findDailyPatientCounts(startDate, endDate);
    }

    private ItemRepository.CategoryCountProjection categoryCount(ItemCategory category, Long totalCount) {
        return new ItemRepository.CategoryCountProjection() {
            @Override
            public ItemCategory getCategory() {
                return category;
            }

            @Override
            public Long getTotalCount() {
                return totalCount;
            }
        };
    }

    private AdminReservationRepository.DailyPatientCountProjection dailyCount(LocalDate date, Long patientCount) {
        return new AdminReservationRepository.DailyPatientCountProjection() {
            @Override
            public LocalDate getDate() {
                return date;
            }

            @Override
            public Long getPatientCount() {
                return patientCount;
            }
        };
    }
}
