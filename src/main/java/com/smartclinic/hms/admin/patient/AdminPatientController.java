package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.AdminPatientDetailResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/patient")
public class AdminPatientController {

    private final AdminPatientService adminPatientService;

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nameKeyword,
            @RequestParam(required = false) String contactKeyword,
            HttpServletRequest req) {
        AdminPatientListResponse result = adminPatientService.getPatientList(page, size, nameKeyword, contactKeyword);
        return renderListPage(req, result);
    }

    String renderListPage(HttpServletRequest req, AdminPatientListResponse res) {
        req.setAttribute("model", res);
        req.setAttribute("pageTitle", "\uD658\uC790 \uAD00\uB9AC");
        return "admin/patient-list";
    }

    String renderDetailPage(HttpServletRequest req, AdminPatientDetailResponse res) {
        req.setAttribute("model", res);
        req.setAttribute("pageTitle", "\uD658\uC790 \uC0C1\uC138");
        return "admin/patient-detail";
    }
}