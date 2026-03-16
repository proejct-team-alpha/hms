package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.CreateAdminStaffRequest;
import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.smartclinic.hms.common.exception.CustomException;

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

    @GetMapping("/new")
    public String newForm(HttpServletRequest req) {
        req.setAttribute("model", adminStaffService.getCreateForm());
        return "admin/staff-form";
    }

    @GetMapping("/form")
    public String form(HttpServletRequest req) {
        return newForm(req);
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute CreateAdminStaffRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest req
    ) {
        if (bindingResult.hasErrors()) {
            req.setAttribute("errorMessage", "입력값을 확인해주세요.");
            req.setAttribute("model", adminStaffService.getCreateForm(request));
            return "admin/staff-form";
        }

        try {
            String successMessage = adminStaffService.createStaff(request);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/admin/staff/list";
        } catch (CustomException ex) {
            req.setAttribute("errorMessage", ex.getMessage());
            req.setAttribute("model", adminStaffService.getCreateForm(request));
            return "admin/staff-form";
        }
    }
}

