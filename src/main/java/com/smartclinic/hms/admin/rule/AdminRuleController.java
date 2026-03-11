package com.smartclinic.hms.admin.rule;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/rule")
public class AdminRuleController {

    @GetMapping("/list")
    public String list() {
        return "admin/rule-list";
    }

    @GetMapping("/form")
    public String form() {
        return "admin/rule-form";
    }
}

