package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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

    private final ReservationRepository reservationRepository;
    private final AdminStaffRepository adminStaffRepository;
    private final ItemRepository itemRepository;
    private final Clock clock;

    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now(clock));
    }

    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {
        return new AdminDashboardStatsResponse(
                reservationRepository.countByReservationDate(today),
                reservationRepository.count(),
                adminStaffRepository.countByActiveTrue(),
                itemRepository.countLowStockItems());
    }

    public AdminDashboardChartResponse getDashboardChart() {
        return getDashboardChart(LocalDate.now(clock));
    }

    public AdminDashboardChartResponse getDashboardChart(LocalDate today) {
        LocalDate startDate = today.minusDays(CHART_RANGE_DAYS);
        LocalDate endDate = today.plusDays(CHART_RANGE_DAYS);

        return new AdminDashboardChartResponse(
                buildCategoryCounts(),
                buildDailyPatients(startDate, endDate));
    }

    private List<AdminDashboardChartResponse.CategoryCount> buildCategoryCounts() {
        return itemRepository.findCategoryCounts()
                .stream()
                .map(c -> new AdminDashboardChartResponse.CategoryCount(
                        toCategoryName(c.getCategory()),
                        c.getTotalCount()))
                .toList();
    }

    private List<AdminDashboardChartResponse.DailyPatientCount> buildDailyPatients(
            LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dailyMap = reservationRepository.findDailyPatientCounts(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        ReservationRepository.DailyPatientCountProjection::getDate,
                        ReservationRepository.DailyPatientCountProjection::getPatientCount));

        return Stream.iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1))
                .map(date -> new AdminDashboardChartResponse.DailyPatientCount(
                        date,
                        dailyMap.getOrDefault(date, 0L)))
                .toList();
    }

    private String toCategoryName(ItemCategory category) {
        return switch (category) {
            case MEDICAL_SUPPLIES -> "의료 소모품";
            case MEDICAL_EQUIPMENT -> "의료 장비";
            case GENERAL_SUPPLIES -> "일반 소모품";
        };
    }
}
