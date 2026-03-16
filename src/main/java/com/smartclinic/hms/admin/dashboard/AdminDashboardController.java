package com.smartclinic.hms.admin.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardStatsService adminDashboardStatsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", adminDashboardStatsService.getDashboardStats());
        model.addAttribute("pageTitle", "관리자 대시보드");
        return "admin/dashboard";
    }
}
