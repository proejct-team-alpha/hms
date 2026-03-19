package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.AdminPatientDetailResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/patient")
public class AdminPatientController {

    private final AdminPatientService adminPatientService;

    String renderListPage(HttpServletRequest req, AdminPatientListResponse model) {
        req.setAttribute("model", model);
        req.setAttribute("pageTitle", "환자 관리");
        return "admin/patient-list";
    }

    String renderDetailPage(HttpServletRequest req, AdminPatientDetailResponse model) {
        req.setAttribute("model", model);
        req.setAttribute("pageTitle", "환자 상세");
        return "admin/patient-detail";
    }
}
