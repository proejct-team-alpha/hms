package com.smartclinic.hms.admin.patient;

import com.smartclinic.hms.admin.patient.dto.AdminPatientDetailResponse;
import com.smartclinic.hms.admin.patient.dto.AdminPatientListResponse;
import com.smartclinic.hms.common.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/patient")
public class AdminPatientController {

    private static final String PATIENT_LIST_TITLE = "환자 관리";
    private static final String PATIENT_DETAIL_TITLE = "환자 상세";
    private static final String NOT_FOUND_TITLE = "페이지를 찾을 수 없습니다";

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

    @GetMapping("/detail")
    public String detail(
            @RequestParam("patientId") Long patientId,
            HttpServletRequest req,
            HttpServletResponse response) {
        try {
            AdminPatientDetailResponse result = adminPatientService.getPatientDetail(patientId);
            return renderDetailPage(req, result);
        } catch (CustomException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            req.setAttribute("pageTitle", NOT_FOUND_TITLE);
            req.setAttribute("errorMessage", ex.getMessage());
            req.setAttribute("path", req.getRequestURI());
            return "error/404";
        }
    }

    String renderListPage(HttpServletRequest req, AdminPatientListResponse res) {
        req.setAttribute("model", res);
        req.setAttribute("pageTitle", PATIENT_LIST_TITLE);
        return "admin/patient-list";
    }

    String renderDetailPage(HttpServletRequest req, AdminPatientDetailResponse res) {
        req.setAttribute("model", res);
        req.setAttribute("pageTitle", PATIENT_DETAIL_TITLE);
        return "admin/patient-detail";
    }
}
