package com.smartclinic.hms.admin.staff;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/staff")
public class AdminStaffController {

    private final AdminStaffRepository adminStaffRepository;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminStaffDto> staffList = adminStaffRepository.findAllWithDepartment()
                .stream()
                .map(AdminStaffDto::new)
                .collect(Collectors.toList());
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
}
