package com.smartclinic.hms.admin.staff;

import com.smartclinic.hms.admin.staff.dto.AdminStaffListResponse;
import com.smartclinic.hms.admin.staff.dto.CreateAdminStaffRequest;
import com.smartclinic.hms.admin.staff.dto.UpdateAdminStaffRequest;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.SsrValidationViewSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "employmentStatus", required = false) String employmentStatus,
            Authentication authentication,
            HttpServletRequest req) {
        AdminStaffListResponse result = adminStaffService.getStaffList(
                page,
                size,
                keyword,
                role,
                employmentStatus,
                authentication.getName());
        req.setAttribute("model", result);
        req.setAttribute("pageTitle", "직원 목록");
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

    @GetMapping("/detail")
    public String detail(@RequestParam("staffId") Long staffId, HttpServletRequest req) {
        req.setAttribute("model", adminStaffService.getEditForm(staffId));
        return "admin/staff-form";
    }

    @PostMapping("/create")
    public String create(
            @Valid @ModelAttribute CreateAdminStaffRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest req) {
        if (bindingResult.hasErrors()) {
            SsrValidationViewSupport.applyErrors(req, bindingResult);
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

    @PostMapping("/update")
    public String update(
            @Valid @ModelAttribute UpdateAdminStaffRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            HttpServletRequest req) {
        if (bindingResult.hasErrors()) {
            SsrValidationViewSupport.applyErrors(req, bindingResult);
            req.setAttribute("model", adminStaffService.getEditForm(request));
            return "admin/staff-form";
        }

        try {
            String successMessage = adminStaffService.updateStaff(request);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/admin/staff/list";
        } catch (CustomException ex) {
            req.setAttribute("errorMessage", ex.getMessage());
            req.setAttribute("model", adminStaffService.getEditForm(request));
            return "admin/staff-form";
        }
    }

    @PostMapping("/deactivate")
    public String deactivate(
            @RequestParam("staffId") Long staffId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            String successMessage = adminStaffService.deactivateStaff(staffId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/staff/list";
    }
}
