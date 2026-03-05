package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.item.ItemRepository;
import com.smartclinic.hms.admin.reservation.AdminReservationRepository;
import com.smartclinic.hms.admin.staff.AdminStaffRepository;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardStatsService {

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
}
