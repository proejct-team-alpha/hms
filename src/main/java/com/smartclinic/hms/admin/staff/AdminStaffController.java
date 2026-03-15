package com.smartclinic.hms.admin.staff;

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
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

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
        model.addAttribute("pageTitle", "직원 등록");
        return "admin/staff-form";
    }

    @PostMapping("/form")
    public String create(@RequestParam String username,
                         @RequestParam String employeeNumber,
                         @RequestParam String password,
                         @RequestParam String name,
                         @RequestParam String role,
                         @RequestParam(required = false) Long departmentId) {
        adminStaffService.createStaff(username, employeeNumber, password, name, role, departmentId);
        return "redirect:/admin/staff/list";
    }
}
