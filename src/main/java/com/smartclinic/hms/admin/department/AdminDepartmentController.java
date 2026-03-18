package com.smartclinic.hms.admin.department;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/department")
public class AdminDepartmentController {

    private final AdminDepartmentService adminDepartmentService;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminDepartmentDto> departments = adminDepartmentService.getDepartmentList();
        model.addAttribute("departments", departments);
        model.addAttribute("hasDepartments", !departments.isEmpty());
        model.addAttribute("pageTitle", "진료과 관리");
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