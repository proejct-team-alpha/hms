package com.smartclinic.hms.admin.staff;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/staff")
public class AdminStaffController {

    @GetMapping("/list")
    public String list() {
        return "admin/staff-list";
    }

    @GetMapping("/form")
    public String form() {
        return "admin/staff-form";
    }
}

