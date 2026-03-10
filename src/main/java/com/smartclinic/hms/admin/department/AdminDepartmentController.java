package com.smartclinic.hms.admin.department;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/department")
public class AdminDepartmentController {

    @GetMapping("/list")
    public String list() {
        return "admin/department-list";
    }
}

