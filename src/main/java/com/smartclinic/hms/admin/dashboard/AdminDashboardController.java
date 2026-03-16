package com.smartclinic.hms.admin.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardStatsService adminDashboardStatsService;

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest req) {
        AdminDashboardStatsResponse stats = adminDashboardStatsService.getDashboardStats();

        req.setAttribute("model", stats);
        return "admin/dashboard";
    }

}