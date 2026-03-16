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
    // 차트 범위
    private static final long CHART_RANGE_DAYS = 3L;

    // repo 선언
    private final AdminReservationRepository adminReservationRepository;
    private final AdminStaffRepository adminStaffRepository;
    private final ItemRepository itemRepository;

    // 관리자 대시보드 날짜 지정
    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now());
    }

    // 관리자 대시보드 통계 조회
    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {
        return new AdminDashboardStatsResponse(
                countTodayReservations(today),
                countTotalReservations(),
                countActiveStaff(),
                countLowStockItems());
    }

    // 관리자 대시보드 날짜 지정
    public AdminDashboardChartResponse getDashboardChart() {
        return getDashboardChart(LocalDate.now());
    }

    // 관리자 대시보드 차트 조회
    public AdminDashboardChartResponse getDashboardChart(LocalDate today) {
        LocalDate startDate = today.minusDays(CHART_RANGE_DAYS);
        LocalDate endDate = today.plusDays(CHART_RANGE_DAYS);

        return new AdminDashboardChartResponse(
                buildCategoryCounts(),
                buildDailyPatients(startDate, endDate));
    }

    // 오늘 날짜의 예약 개수
    private long countTodayReservations(LocalDate today) {
        return adminReservationRepository.countByReservationDate(today);
    }

    // 총 예약 수 조회
    private long countTotalReservations() {
        return adminReservationRepository.count();
    }

    // 활성화된 직원 수 조회
    private long countActiveStaff() {
        return adminStaffRepository.countByActiveTrue();
    }

    // 재고 수 조회
    private long countLowStockItems() {
        return itemRepository.countLowStockItems();
    }

    // 카테고리별 카운트
    private List<AdminDashboardChartResponse.CategoryCount> buildCategoryCounts() {
        return itemRepository.findCategoryCounts()
                .stream()
                .map(categoryCount -> new AdminDashboardChartResponse.CategoryCount(
                        toCategoryName(categoryCount.getCategory()),
                        categoryCount.getTotalCount()))
                .toList();
    }

    // 일별 환자 수
    private List<AdminDashboardChartResponse.DailyPatientCount> buildDailyPatients(
            LocalDate startDate,
            LocalDate endDate) {
        Map<LocalDate, Long> dailyPatientCounts = adminReservationRepository.findDailyPatientCounts(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        projection -> projection.getDate(),
                        projection -> projection.getPatientCount()));

        return Stream
                .iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1))
                .map(date -> new AdminDashboardChartResponse.DailyPatientCount(
                        date,
                        dailyPatientCounts.getOrDefault(date, 0L)))
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