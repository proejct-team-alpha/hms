package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.item.log.ItemStockLog;
import com.smartclinic.hms.item.log.ItemStockLogRepository;
import com.smartclinic.hms.item.log.ItemStockType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardStatsService {
    private static final long DAILY_PATIENT_RANGE_DAYS = 3L;
    private static final int ITEM_FLOW_CHART_DAYS = 7;
    private static final DateTimeFormatter ITEM_FLOW_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM/dd");

    private final AdminReservationRepository adminReservationRepository;
    private final AdminStaffRepository adminStaffRepository;
    private final ItemRepository itemRepository;
    private final ItemStockLogRepository itemStockLogRepository;

    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now());
    }

    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {
        return new AdminDashboardStatsResponse(
                countTodayReservations(today),
                countTotalReservations(),
                countActiveStaff(),
                countLowStockItems());
    }

    public AdminDashboardChartResponse getDashboardChart() {
        return getDashboardChart(LocalDate.now());
    }

    public AdminDashboardChartResponse getDashboardChart(LocalDate today) {
        LocalDate patientStartDate = today.minusDays(DAILY_PATIENT_RANGE_DAYS);
        LocalDate patientEndDate = today.plusDays(DAILY_PATIENT_RANGE_DAYS);

        return new AdminDashboardChartResponse(
                buildItemFlowDays(today),
                buildDailyPatients(patientStartDate, patientEndDate));
    }

    private long countTodayReservations(LocalDate today) {
        return adminReservationRepository.countByReservationDate(today);
    }

    private long countTotalReservations() {
        return adminReservationRepository.count();
    }

    private long countActiveStaff() {
        return adminStaffRepository.countByActiveTrue();
    }

    private long countLowStockItems() {
        return itemRepository.countLowStockItems();
    }

    private List<AdminDashboardChartResponse.ItemFlowDay> buildItemFlowDays(LocalDate today) {
        LocalDateTime start = today.minusDays(ITEM_FLOW_CHART_DAYS - 1L).atStartOfDay();
        LocalDateTime endExclusive = today.plusDays(1).atStartOfDay();
        List<ItemStockLog> logs = itemStockLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, endExclusive);

        Map<LocalDate, int[]> dailyFlowMap = new LinkedHashMap<>();
        for (int i = ITEM_FLOW_CHART_DAYS - 1; i >= 0; i--) {
            dailyFlowMap.put(today.minusDays(i), new int[]{0, 0});
        }

        for (ItemStockLog log : logs) {
            LocalDate logDate = log.getCreatedAt().toLocalDate();
            if (!dailyFlowMap.containsKey(logDate)) {
                continue;
            }

            int[] amounts = dailyFlowMap.get(logDate);
            if (log.getType() == ItemStockType.IN) {
                amounts[0] += log.getAmount();
            } else {
                amounts[1] += log.getAmount();
            }
        }

        int maxAmount = dailyFlowMap.values().stream()
                .mapToInt(amounts -> Math.max(amounts[0], amounts[1]))
                .max()
                .orElse(0);

        return dailyFlowMap.entrySet().stream()
                .map(entry -> toItemFlowDay(entry, maxAmount))
                .toList();
    }

    private AdminDashboardChartResponse.ItemFlowDay toItemFlowDay(Map.Entry<LocalDate, int[]> entry, int maxAmount) {
        int inAmount = entry.getValue()[0];
        int outAmount = entry.getValue()[1];
        int inHeight = maxAmount > 0 ? (int) Math.round(inAmount * 100.0 / maxAmount) : 0;
        int outHeight = maxAmount > 0 ? (int) Math.round(outAmount * 100.0 / maxAmount) : 0;

        return new AdminDashboardChartResponse.ItemFlowDay(
                entry.getKey().format(ITEM_FLOW_LABEL_FORMAT),
                inAmount,
                outAmount,
                inHeight,
                outHeight);
    }

    private List<AdminDashboardChartResponse.DailyPatientCount> buildDailyPatients(
            LocalDate startDate,
            LocalDate endDate) {
        Map<LocalDate, Long> dailyPatientCounts = adminReservationRepository.findDailyPatientCounts(startDate, endDate)
                .stream()
                .collect(Collectors.toMap(
                        AdminReservationRepository.DailyPatientCountProjection::getDate,
                        AdminReservationRepository.DailyPatientCountProjection::getPatientCount));

        return Stream
                .iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1))
                .map(date -> new AdminDashboardChartResponse.DailyPatientCount(
                        date,
                        dailyPatientCounts.getOrDefault(date, 0L)))
                .toList();
    }
}