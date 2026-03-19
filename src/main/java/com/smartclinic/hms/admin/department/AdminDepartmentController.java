package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.common.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/department")
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest req) {
        return renderListPage(req, adminDepartmentService.getDepartmentList(page, size));
    }

    @GetMapping("/detail")
    public String detail(
            @RequestParam("departmentId") Long departmentId,
            HttpServletRequest req,
            HttpServletResponse response) {
        try {
            req.setAttribute("model", adminDepartmentService.getDepartmentDetail(departmentId));
            req.setAttribute("pageTitle", "\uC9C4\uB8CC\uACFC \uC0C1\uC138");
            return "admin/department-detail";
        } catch (CustomException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            req.setAttribute("pageTitle", "\uD398\uC774\uC9C0\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4");
            req.setAttribute("errorMessage", ex.getMessage());
            req.setAttribute("path", req.getRequestURI());
            return "error/404";
        }
    }

    @PostMapping("/create")
    public Object create(
            @Valid @ModelAttribute CreateAdminDepartmentRequest request,
            BindingResult bindingResult,
            HttpServletRequest req) {
        if (bindingResult.hasErrors()) {
            return renderCreateValidationFailure(req, request, bindingResult);
        }

        adminDepartmentService.createDepartment(request.getName(), request.isActive());
        return redirectTo("/admin/department/list");
    }

    @PostMapping("/update")
    public RedirectView update(
            @RequestParam Long departmentId,
            @RequestParam(defaultValue = "") String name,
            RedirectAttributes redirectAttributes) {
        try {
            String successMessage = adminDepartmentService.updateDepartmentName(departmentId, name);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return redirectToDetail(departmentId);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (ex.getHttpStatus() == HttpStatus.NOT_FOUND) {
                return redirectTo("/admin/department/list");
            }
            return redirectToDetail(departmentId);
        }
    }

    @PostMapping("/deactivate")
    public RedirectView deactivate(
            @RequestParam Long departmentId,
            RedirectAttributes redirectAttributes) {
        try {
            String successMessage = adminDepartmentService.deactivateDepartment(departmentId);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return redirectToDetail(departmentId);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (ex.getHttpStatus() == HttpStatus.NOT_FOUND) {
                return redirectTo("/admin/department/list");
            }
            return redirectToDetail(departmentId);
        }
    }

    @PostMapping("/activate")
    public RedirectView activate(
            @RequestParam Long departmentId,
            RedirectAttributes redirectAttributes) {
        try {
            String successMessage = adminDepartmentService.activateDepartment(departmentId);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return redirectToDetail(departmentId);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (ex.getHttpStatus() == HttpStatus.NOT_FOUND) {
                return redirectTo("/admin/department/list");
            }
            return redirectToDetail(departmentId);
        }
    }

    private String renderListPage(HttpServletRequest req, AdminDepartmentListResponse model) {
        req.setAttribute("model", model);
        req.setAttribute("pageTitle", "\uC9C4\uB8CC\uACFC \uAD00\uB9AC");
        req.setAttribute("createName", "");
        req.setAttribute("createActive", true);
        return "admin/department-list";
    }

    private String renderCreateValidationFailure(
            HttpServletRequest req,
            CreateAdminDepartmentRequest request,
            BindingResult bindingResult) {
        req.setAttribute("model", adminDepartmentService.getDepartmentList(1, 10));
        req.setAttribute("pageTitle", "\uC9C4\uB8CC\uACFC \uAD00\uB9AC");
        req.setAttribute("createName", request.getName());
        req.setAttribute("createActive", request.isActive());
        req.setAttribute("openCreateModal", true);
        req.setAttribute("nameError", getFieldError(bindingResult, "name"));
        return "admin/department-list";
    }

    private String getFieldError(BindingResult bindingResult, String fieldName) {
        if (bindingResult.getFieldError(fieldName) == null) {
            return null;
        }
        return bindingResult.getFieldError(fieldName).getDefaultMessage();
    }

    private RedirectView redirectToDetail(Long departmentId) {
        return redirectTo("/admin/department/detail?departmentId=" + departmentId);
    }

    private RedirectView redirectTo(String url) {
        RedirectView redirectView = new RedirectView(url);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
