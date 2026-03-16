package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String employmentStatus,
            HttpServletRequest req
    ) {
        AdminStaffListResponse result = adminStaffService.getStaffList(page, size, keyword, role, employmentStatus);
        req.setAttribute("model", result);
        return "admin/staff-list";
    }

    @GetMapping("/form")
    public String form() {
        return "admin/staff-form";
    }
}

