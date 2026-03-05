package com.smartclinic.hms.admin.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard");
        model.addAttribute("totalStaff", 0);
        model.addAttribute("todayCancel", 0);
        model.addAttribute("activeDepts", 0);
        model.addAttribute("activeRules", 0);
        model.addAttribute("patientChartLabels", "[]");
        model.addAttribute("patientChartData", "[]");
        model.addAttribute("inventoryChartLabels", "[]");
        model.addAttribute("inventoryChartData", "[]");
        return "admin/dashboard";
    }
}
