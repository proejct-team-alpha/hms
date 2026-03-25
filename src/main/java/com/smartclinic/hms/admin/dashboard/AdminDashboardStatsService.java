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

    // LocalDate.now()를 사용하여 오늘 날짜 기준의 대시보드 통계를 조회하고,
    // 내부 오버로딩 메서드(getDashboardStats(LocalDate))로 위임한다.
    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now());
    }

    // 기준 날짜(today)를 받아 해당 날짜 기준 대시보드 통계를 조회한다.
    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {
        return new AdminDashboardStatsResponse(
                countTodayReservations(today),
                countTotalReservations(),
                countActiveStaff(),
                countLowStockItems());
    }

    // LocalDate.now()를 사용하여 오늘 날짜 기준의 차트 데이터를 조회하고,
    // 내부 메서드(getDashboardChart(LocalDate))로 위임한다.
    public AdminDashboardChartResponse getDashboardChart() {
        return getDashboardChart(LocalDate.now());
    }

    // 기준 날짜(today)를 기준으로 대시보드 차트 데이터를 조회한다.
    // 차트 렌더링을 위한 데이터(ItemFlowDays, DailyPatients)를 생성하여 반환한다.
    // 내부에서 사용하는 메서드는 보조 계산용이며 최종 통계 DTO에는 포함되지 않는다.
    public AdminDashboardChartResponse getDashboardChart(LocalDate today) {
        LocalDate patientStartDate = today.minusDays(DAILY_PATIENT_RANGE_DAYS);
        LocalDate patientEndDate = today.plusDays(DAILY_PATIENT_RANGE_DAYS);

        return new AdminDashboardChartResponse(
                buildItemFlowDays(today),
                buildDailyPatients(patientStartDate, patientEndDate));
    }

    // 오늘 예약수 집계
    private long countTodayReservations(LocalDate today) {
        return adminReservationRepository.countByReservationDate(today);
    }

    // 총 예약수 집계
    private long countTotalReservations() {
        return adminReservationRepository.count();
    }

    // 활성화 중인 직원수 집계
    private long countActiveStaff() {
        return adminStaffRepository.countByActiveTrue();
    }

    // Item 엔티티를 대상으로 현재 수량(quantity)이 최소 수량(minQuantity)보다 작은 물품을
    // JPQL의 count 집계 함수를 사용해 개수로 조회하고 반환한다.
    private long countLowStockItems() {
        return itemRepository.countLowStockItems();
    }

    // 오늘을 기준으로 7일간의 입고/출고 데이터를 날짜별로 Map에 저장하고,
    // 해당 Map과 전체 기간의 최대 입출고 수치를 함께 반환한다.
    private List<AdminDashboardChartResponse.ItemFlowDay> buildItemFlowDays(LocalDate today) {
        LocalDateTime start = today.minusDays(ITEM_FLOW_CHART_DAYS - 1L).atStartOfDay();
        LocalDateTime endExclusive = today.plusDays(1).atStartOfDay();
        List<ItemStockLog> logs = itemStockLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, endExclusive);

        Map<LocalDate, int[]> dailyFlowMap = new LinkedHashMap<>();
        for (int i = ITEM_FLOW_CHART_DAYS - 1; i >= 0; i--) {
            dailyFlowMap.put(today.minusDays(i), new int[] { 0, 0 });
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

    // entry의 입고/출고 데이터를 maxAmount 기준으로 정규화하여
    // ItemFlowDay DTO로 변환하는 함수
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

    // 시작일 ~ 종료일 기간의 날짜별 예약 수를 조회한다.
    // DB 집계 결과에는 예약이 없는 날짜가 포함되지 않으므로,
    // 전체 기간을 순회하며 없는 날짜는 0건으로 보정하여 반환한다.
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
