package com.smartclinic.hms.admin.staff;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smartclinic.hms.admin.department.AdminDepartmentService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;
    private final AdminDepartmentService adminDepartmentService;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminStaffDto> staffList = adminStaffService.getStaffList();
        model.addAttribute("staffList", staffList);
        model.addAttribute("hasStaff", !staffList.isEmpty());
        model.addAttribute("pageTitle", "직원 관리");
        return "admin/staff-list";
    }

    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("departments", adminDepartmentService.getDepartmentList());
        model.addAttribute("pageTitle", "직원 등록");
        return "admin/staff-form";
    }

    @PostMapping("/form")
    public String create(@RequestParam("username") String username,
                         @RequestParam("employeeNumber") String employeeNumber,
                         @RequestParam("password") String password,
                         @RequestParam("name") String name,
                         @RequestParam("role") String role,
                         @RequestParam(name = "departmentId", required = false) Long departmentId) {
        adminStaffService.createStaff(username, employeeNumber, password, name, role, departmentId);
        return "redirect:/admin/staff/list";
    }
}
