package com.smartclinic.hms.admin.department;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
