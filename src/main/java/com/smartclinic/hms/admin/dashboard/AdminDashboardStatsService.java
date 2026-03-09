package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.domain.ItemCategory;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardStatsService {
    private static final long CHART_RANGE_DAYS = 3L;

    private final AdminReservationRepository adminReservationRepository;
    private final AdminStaffRepository adminStaffRepository;
    private final ItemRepository itemRepository;

    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now());
    }

    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {
        long lowStockItemCount = itemRepository.findAllProjectedBy()
                .stream()
                .filter(item -> item.getQuantity() < item.getMinQuantity())
                .count();

        return new AdminDashboardStatsResponse(
                adminReservationRepository.countByReservationDate(today),
                adminReservationRepository.count(),
                adminStaffRepository.countByActiveTrue(),
                lowStockItemCount);
    }

    public AdminDashboardChartResponse getDashboardChart() {
        return getDashboardChart(LocalDate.now());
    }

    public AdminDashboardChartResponse getDashboardChart(LocalDate today) {
        LocalDate startDate = today.minusDays(CHART_RANGE_DAYS);
        LocalDate endDate = today.plusDays(CHART_RANGE_DAYS);

        List<AdminDashboardChartResponse.CategoryCount> categoryCounts = itemRepository.findCategoryCounts()
                .stream()
                .map(item -> new AdminDashboardChartResponse.CategoryCount(
                        toCategoryName(item.getCategory()),
                        item.getTotalCount()))
                .toList();

        Map<LocalDate, Long> dailyPatientCounts = adminReservationRepository.findDailyPatientCounts(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        AdminReservationRepository.DailyPatientCountProjection::getDate,
                        AdminReservationRepository.DailyPatientCountProjection::getPatientCount));

        List<AdminDashboardChartResponse.DailyPatientCount> dailyPatients = Stream
                .iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1))
                .map(date -> new AdminDashboardChartResponse.DailyPatientCount(
                        date,
                        dailyPatientCounts.getOrDefault(date, 0L)))
                .toList();

        return new AdminDashboardChartResponse(categoryCounts, dailyPatients);
    }

    private String toCategoryName(ItemCategory category) {
        return switch (category) {
            case MEDICAL_SUPPLIES -> "의료 소모품";
            case MEDICAL_EQUIPMENT -> "의료 장비";
            case GENERAL_SUPPLIES -> "일반 소모품";
        };
    }
}
