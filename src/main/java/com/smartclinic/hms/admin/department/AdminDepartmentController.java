package com.smartclinic.hms.admin.department;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;

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

    @PostMapping("/form")
    public String create(@RequestParam String name) {
        adminDepartmentService.createDepartment(name);
        return "redirect:/admin/department/list";
    }
}