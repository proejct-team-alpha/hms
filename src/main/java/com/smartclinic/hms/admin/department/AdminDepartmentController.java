package com.smartclinic.hms.admin.department;

import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/department")
public class AdminDepartmentController {

    private final DepartmentRepository departmentRepository;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminDepartmentDto> departments = departmentRepository.findAll()
                .stream()
                .map(AdminDepartmentDto::new)
                .collect(Collectors.toList());
        model.addAttribute("departments", departments);
        model.addAttribute("hasDepartments", !departments.isEmpty());
        model.addAttribute("pageTitle", "진료과 관리");
        return "admin/department-list";
    }
}
