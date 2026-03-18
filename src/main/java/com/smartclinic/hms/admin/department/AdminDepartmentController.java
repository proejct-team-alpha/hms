package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.common.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        req.setAttribute("model", adminDepartmentService.getDepartmentList(page, size));
        req.setAttribute("pageTitle", "진료과 관리");
        return "admin/department-list";
    }

    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("pageTitle", "진료과 등록");
        return "admin/department-form";
    }

    @GetMapping("/detail")
    public String detail(
            @RequestParam("departmentId") Long departmentId,
            HttpServletRequest req,
            HttpServletResponse response) {
        try {
            req.setAttribute("model", adminDepartmentService.getDepartmentDetail(departmentId));
            req.setAttribute("pageTitle", "진료과 상세");
            return "admin/department-detail";
        } catch (CustomException ex) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            req.setAttribute("pageTitle", "페이지를 찾을 수 없습니다");
            req.setAttribute("errorMessage", ex.getMessage());
            req.setAttribute("path", req.getRequestURI());
            return "error/404";
        }
    }

    @PostMapping("/form")
    public RedirectView create(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean active) {
        adminDepartmentService.createDepartment(name, active);
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

    private RedirectView redirectToDetail(Long departmentId) {
        return redirectTo("/admin/department/detail?departmentId=" + departmentId);
    }

    private RedirectView redirectTo(String url) {
        RedirectView redirectView = new RedirectView(url);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}