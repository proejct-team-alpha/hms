package com.smartclinic.hms.nurse;

import com.smartclinic.hms.nurse.dto.NurseDashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseDashboardController {

    private final NurseService nurseService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        NurseDashboardDto dashboard = nurseService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("pageTitle", "간호사 대시보드");
        return "nurse/dashboard";
    }
}
