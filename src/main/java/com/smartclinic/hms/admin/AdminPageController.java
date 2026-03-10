package com.smartclinic.hms.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/reservation-list")
    public String reservationList() {
        return "admin/reservation-list";
    }

    @GetMapping("/department-list")
    public String departmentList() {
        return "admin/department-list";
    }

    @GetMapping("/rule-list")
    public String ruleList() {
        return "admin/rule-list";
    }

    @GetMapping("/rule-form")
    public String ruleForm() {
        return "admin/rule-form";
    }

    @GetMapping("/staff-list")
    public String staffList() {
        return "admin/staff-list";
    }

    @GetMapping("/staff-form")
    public String staffForm() {
        return "admin/staff-form";
    }

    @GetMapping("/item-list")
    public String itemList() {
        return "admin/item-list";
    }

    @GetMapping("/item-form")
    public String itemForm() {
        return "admin/item-form";
    }
}

