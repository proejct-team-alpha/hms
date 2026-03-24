package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.item.log.ItemStockLog;
import com.smartclinic.hms.item.log.ItemStockLogRepository;
import com.smartclinic.hms.item.log.ItemStockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

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

    @Mock
    private ItemStockLogRepository itemStockLogRepository;

    @InjectMocks
    private AdminDashboardStatsService adminDashboardStatsService;

    @Test
    @DisplayName("dashboard stats returns four aggregated metrics")
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
    @DisplayName("chart response keeps seven patient days and item flow days")
    void getDashboardChart_buildsDailyPatientsAndItemFlowDays() {
        // given
        LocalDate today = LocalDate.of(2026, 3, 5);
        LocalDate patientStartDate = today.minusDays(3);
        LocalDate patientEndDate = today.plusDays(3);
        LocalDateTime itemFlowStart = today.minusDays(6).atStartOfDay();
        LocalDateTime itemFlowEndExclusive = today.plusDays(1).atStartOfDay();

        given(adminReservationRepository.findDailyPatientCounts(patientStartDate, patientEndDate)).willReturn(List.of(
                dailyCount(today.minusDays(2), 3L),
                dailyCount(today, 4L),
                dailyCount(today.plusDays(1), 1L)));
        given(itemStockLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(itemFlowStart, itemFlowEndExclusive))
                .willReturn(List.of(
                        stockLog(today.minusDays(6).atTime(9, 0), ItemStockType.IN, 2),
                        stockLog(today.minusDays(6).atTime(11, 0), ItemStockType.OUT, 1),
                        stockLog(today.minusDays(3).atTime(14, 0), ItemStockType.IN, 5),
                        stockLog(today.atTime(10, 0), ItemStockType.OUT, 4)));

        // when
        AdminDashboardChartResponse response = adminDashboardStatsService.getDashboardChart(today);

        // then
        assertThat(response.dailyPatients()).hasSize(7);
        assertThat(response.dailyPatients().get(0).date()).isEqualTo(today.minusDays(3));
        assertThat(response.dailyPatients().get(0).patientCount()).isEqualTo(0L);
        assertThat(response.dailyPatients().get(1).date()).isEqualTo(today.minusDays(2));
        assertThat(response.dailyPatients().get(1).patientCount()).isEqualTo(3L);
        assertThat(response.dailyPatients().get(3).date()).isEqualTo(today);
        assertThat(response.dailyPatients().get(3).patientCount()).isEqualTo(4L);
        assertThat(response.dailyPatients().get(6).date()).isEqualTo(today.plusDays(3));
        assertThat(response.dailyPatients().get(6).patientCount()).isEqualTo(0L);

        assertThat(response.itemFlowDays()).hasSize(7);
        assertThat(response.itemFlowDays().get(0).label()).isEqualTo(today.minusDays(6).format(DateTimeFormatter.ofPattern("MM/dd")));
        assertThat(response.itemFlowDays().get(0).inAmount()).isEqualTo(2);
        assertThat(response.itemFlowDays().get(0).outAmount()).isEqualTo(1);
        assertThat(response.itemFlowDays().get(0).inHeight()).isEqualTo(40);
        assertThat(response.itemFlowDays().get(3).label()).isEqualTo(today.minusDays(3).format(DateTimeFormatter.ofPattern("MM/dd")));
        assertThat(response.itemFlowDays().get(3).inAmount()).isEqualTo(5);
        assertThat(response.itemFlowDays().get(3).outAmount()).isEqualTo(0);
        assertThat(response.itemFlowDays().get(3).inHeight()).isEqualTo(100);
        assertThat(response.itemFlowDays().get(6).label()).isEqualTo(today.format(DateTimeFormatter.ofPattern("MM/dd")));
        assertThat(response.itemFlowDays().get(6).inAmount()).isEqualTo(0);
        assertThat(response.itemFlowDays().get(6).outAmount()).isEqualTo(4);
        assertThat(response.itemFlowDays().get(6).outHeight()).isEqualTo(80);

        then(adminReservationRepository).should().findDailyPatientCounts(patientStartDate, patientEndDate);
        then(itemStockLogRepository).should().findByCreatedAtBetweenOrderByCreatedAtAsc(itemFlowStart, itemFlowEndExclusive);
    }

    @Test
    @DisplayName("chart response zero-fills item flow days when no stock logs exist")
    void getDashboardChart_returnsZeroFilledItemFlowDaysWhenNoLogs() {
        // given
        LocalDate today = LocalDate.of(2026, 3, 5);
        LocalDate patientStartDate = today.minusDays(3);
        LocalDate patientEndDate = today.plusDays(3);
        LocalDateTime itemFlowStart = today.minusDays(6).atStartOfDay();
        LocalDateTime itemFlowEndExclusive = today.plusDays(1).atStartOfDay();

        given(adminReservationRepository.findDailyPatientCounts(patientStartDate, patientEndDate))
                .willReturn(Collections.emptyList());
        given(itemStockLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(itemFlowStart, itemFlowEndExclusive))
                .willReturn(Collections.emptyList());

        // when
        AdminDashboardChartResponse response = adminDashboardStatsService.getDashboardChart(today);

        // then
        assertThat(response.itemFlowDays()).hasSize(7);
        assertThat(response.itemFlowDays())
                .extracting(AdminDashboardChartResponse.ItemFlowDay::label)
                .containsExactly(
                        today.minusDays(6).format(DateTimeFormatter.ofPattern("MM/dd")),
                        today.minusDays(5).format(DateTimeFormatter.ofPattern("MM/dd")),
                        today.minusDays(4).format(DateTimeFormatter.ofPattern("MM/dd")),
                        today.minusDays(3).format(DateTimeFormatter.ofPattern("MM/dd")),
                        today.minusDays(2).format(DateTimeFormatter.ofPattern("MM/dd")),
                        today.minusDays(1).format(DateTimeFormatter.ofPattern("MM/dd")),
                        today.format(DateTimeFormatter.ofPattern("MM/dd")));
        assertThat(response.itemFlowDays())
                .allSatisfy(itemFlowDay -> {
                    assertThat(itemFlowDay.inAmount()).isZero();
                    assertThat(itemFlowDay.outAmount()).isZero();
                    assertThat(itemFlowDay.inHeight()).isZero();
                    assertThat(itemFlowDay.outHeight()).isZero();
                });
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

    private ItemStockLog stockLog(LocalDateTime createdAt, ItemStockType type, int amount) {
        ItemStockLog log = ItemStockLog.of(1L, "mask", type, amount, "admin");
        ReflectionTestUtils.setField(log, "createdAt", createdAt);
        return log;
    }
}