package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardStatsService {

    private final AdminDashboardStatsRepository adminDashboardStatsRepository;

    public AdminDashboardStatsResponse getDashboardStats() {
        return getDashboardStats(LocalDate.now());
    }

    public AdminDashboardStatsResponse getDashboardStats(LocalDate today) {
        return new AdminDashboardStatsResponse(
                adminDashboardStatsRepository.countReservationsByDate(today),
                adminDashboardStatsRepository.countAllReservations(),
                adminDashboardStatsRepository.countActiveStaff(),
                adminDashboardStatsRepository.countLowStockItems());
    }
}
