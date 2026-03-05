package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
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
        given(itemRepository.findAllProjectedBy()).willReturn(java.util.List.of(
                stock(2, 5),
                stock(20, 10)
        ));

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
        then(itemRepository).should().findAllProjectedBy();
    }

    private ItemRepository.StockLevelProjection stock(int quantity, int minQuantity) {
        return new ItemRepository.StockLevelProjection() {
            @Override
            public int getQuantity() {
                return quantity;
            }

            @Override
            public int getMinQuantity() {
                return minQuantity;
            }
        };
    }
}
