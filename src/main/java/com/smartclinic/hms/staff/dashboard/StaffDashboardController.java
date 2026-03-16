package com.smartclinic.hms.staff.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartclinic.hms.staff.reception.ReceptionService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff")
public class StaffDashboardController {

    private final ReceptionService receptionService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", receptionService.getDashboard());
        model.addAttribute("pageTitle", "접수 대시보드");
        return "staff/dashboard";
    }
}
