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
    // 날짜 범위
    private static final long CHART_RANGE_DAYS = 3L;

    // Repository
    private final AdminReservationRepository adminReservationRepository;
    private final AdminStaffRepository adminStaffRepository;
    private final ItemRepository itemRepository;

    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now());
    }

    // 대시보드드 통계 정보 반환
    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {

        // 재고 부족 아이템 개수 계산산
        long lowStockItemCount = itemRepository.findAllProjectedBy()
                .stream()
                .filter(item -> item.getQuantity() < item.getMinQuantity())
                .count();

        // 관리자 대시보드 통계 정보 반환
        return new AdminDashboardStatsResponse(
                adminReservationRepository.countByReservationDate(today),
                adminReservationRepository.count(),
                adminStaffRepository.countByActiveTrue(),
                lowStockItemCount);
    }

    // 대시보드 차트 정보 반환 날짜 기본값 오늘
    public AdminDashboardChartResponse getDashboardChart() {
        return getDashboardChart(LocalDate.now());
    }

    // 대시보드 차트 정보 반환
    public AdminDashboardChartResponse getDashboardChart(LocalDate today) {
        // START 날짜 END 날짜 설정
        LocalDate startDate = today.minusDays(CHART_RANGE_DAYS);
        LocalDate endDate = today.plusDays(CHART_RANGE_DAYS);

        // 카테고리별 아이템 개수 계산
        List<AdminDashboardChartResponse.CategoryCount> categoryCounts = itemRepository.findCategoryCounts()
                .stream()
                .map(item -> new AdminDashboardChartResponse.CategoryCount(
                        toCategoryName(item.getCategory()),
                        item.getTotalCount()))
                .toList();
        // 일별 예약자 수 계산
        Map<LocalDate, Long> dailyPatientCounts = adminReservationRepository.findDailyPatientCounts(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        AdminReservationRepository.DailyPatientCountProjection::getDate,
                        AdminReservationRepository.DailyPatientCountProjection::getPatientCount));
        // 차트 데이터 생성
        List<AdminDashboardChartResponse.DailyPatientCount> dailyPatients = Stream
                .iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1))
                .map(date -> new AdminDashboardChartResponse.DailyPatientCount(
                        date,
                        dailyPatientCounts.getOrDefault(date, 0L)))
                .toList();

        return new AdminDashboardChartResponse(categoryCounts, dailyPatients);
    }

    // 카테고리 이름 변환
    private String toCategoryName(ItemCategory category) {
        return switch (category) {
            case MEDICAL_SUPPLIES -> "의료 소모품";
            case MEDICAL_EQUIPMENT -> "의료 장비";
            case GENERAL_SUPPLIES -> "일반 소모품";
        };
    }
}
