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
    public String detail(
            @RequestParam("staffId") Long staffId,
            Authentication authentication,
            HttpServletRequest req,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            req.setAttribute("model", adminStaffService.getEditForm(staffId, authentication.getName()));
            return "admin/staff-form";
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/staff/list";
        }
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
            applyCreateViewErrors(req, request, ex);
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
            Authentication authentication,
            HttpServletRequest req) {
        if (bindingResult.hasErrors()) {
            SsrValidationViewSupport.applyErrors(req, bindingResult);
            req.setAttribute("model", adminStaffService.getEditForm(request, authentication.getName()));
            return "admin/staff-form";
        }

        try {
            String successMessage = adminStaffService.updateStaff(request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return "redirect:/admin/staff/list";
        } catch (CustomException ex) {
            applyUpdateViewErrors(req, ex);
            req.setAttribute("errorMessage", ex.getMessage());
            req.setAttribute("model", adminStaffService.getEditForm(request, authentication.getName()));
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

    private void applyCreateViewErrors(HttpServletRequest req, CreateAdminStaffRequest request, CustomException ex) {
        if (!"VALIDATION_ERROR".equals(ex.getErrorCode())) {
            return;
        }

        if ("DOCTOR".equalsIgnoreCase(request.role()) && containsDepartmentMessage(ex.getMessage())) {
            req.setAttribute("departmentIdError", ex.getMessage());
        }

        if (containsRetiredAtMessage(ex.getMessage())) {
            req.setAttribute("retiredAtError", ex.getMessage());
        }
    }

    private void applyUpdateViewErrors(HttpServletRequest req, CustomException ex) {
        if (!"VALIDATION_ERROR".equals(ex.getErrorCode())) {
            return;
        }

        if (containsDepartmentMessage(ex.getMessage())) {
            req.setAttribute("departmentIdError", ex.getMessage());
        }

        if (containsEmploymentStatusMessage(ex.getMessage())) {
            req.setAttribute("activeError", ex.getMessage());
        }

        if (containsRetiredAtMessage(ex.getMessage())) {
            req.setAttribute("retiredAtError", ex.getMessage());
        }
    }

    private boolean containsDepartmentMessage(String message) {
        return message != null && message.contains("부서");
    }

    private boolean containsEmploymentStatusMessage(String message) {
        return message != null
                && (message.contains("비활성화")
                || message.contains("재활성화")
                || message.contains("활성화"));
    }

    private boolean containsRetiredAtMessage(String message) {
        return message != null && message.contains("퇴사");
    }
}
