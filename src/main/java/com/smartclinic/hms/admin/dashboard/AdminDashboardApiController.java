package com.smartclinic.hms.admin.dashboard;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardChartResponse;
import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;
import com.smartclinic.hms.common.util.Resp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/dashboard")
public class AdminDashboardApiController {

    private final AdminDashboardStatsService adminDashboardStatsService;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {

        AdminDashboardStatsResponse respDTO = adminDashboardStatsService.getDashboardStats();

        return Resp.ok(respDTO);
    }

    @GetMapping("/chart")
    public ResponseEntity<Resp<AdminDashboardChartResponse>> chart() {
        AdminDashboardChartResponse chartResponse = adminDashboardStatsService.getDashboardChart();
        return Resp.ok(chartResponse);
    }
}