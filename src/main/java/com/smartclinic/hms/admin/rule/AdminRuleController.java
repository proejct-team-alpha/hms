package com.smartclinic.hms.admin.rule;

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
@RequestMapping("/admin/rule")
public class AdminRuleController {

    private final AdminRuleService adminRuleService;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminRuleDto> rules = adminRuleService.getRuleList();
        model.addAttribute("rules", rules);
        model.addAttribute("hasRules", !rules.isEmpty());
        model.addAttribute("pageTitle", "병원 규칙 관리");
        return "admin/rule-list";
    }

    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("pageTitle", "규칙 등록");
        return "admin/rule-form";
    }

    @PostMapping("/form")
    public String create(@RequestParam String title,
                         @RequestParam String content,
                         @RequestParam String category) {
        adminRuleService.createRule(title, content, category);
        return "redirect:/admin/rule/list";
    }
}
