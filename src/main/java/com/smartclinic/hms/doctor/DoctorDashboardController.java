package com.smartclinic.hms.doctor;

import com.smartclinic.hms.doctor.treatment.DoctorTreatmentService;
import com.smartclinic.hms.doctor.treatment.dto.DoctorDashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorDashboardController {

    private final DoctorTreatmentService treatmentService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        DoctorDashboardDto dashboard = treatmentService.getDashboardData(auth.getName());
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("pageTitle", "의사 대시보드");
        return "doctor/dashboard";
    }
}
