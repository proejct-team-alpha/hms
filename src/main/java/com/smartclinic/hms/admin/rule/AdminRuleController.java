package com.smartclinic.hms.admin.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rule")
public class AdminRuleController {

    private static final String RULE_LIST_TITLE = "병원 규칙 관리";
    private static final String RULE_FORM_TITLE = "규칙 등록";

    private final AdminRuleService adminRuleService;

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String category,
            @RequestParam(defaultValue = "ALL") String active,
            @RequestParam(required = false) String keyword,
            Model model) {
        AdminRuleListResponse result = adminRuleService.getRuleList(page, size, category, active, keyword);
        model.addAttribute("model", result);
        model.addAttribute("rules", result.rules());
        model.addAttribute("hasRules", !result.rules().isEmpty());
        model.addAttribute("pageTitle", RULE_LIST_TITLE);
        return "admin/rule-list";
    }

    @GetMapping("/form")
    public String form(Model model) {
        model.addAttribute("pageTitle", RULE_FORM_TITLE);
        return "admin/rule-form";
    }

    @PostMapping("/form")
    public String create(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category) {
        adminRuleService.createRule(title, content, category);
        return "redirect:/admin/rule/list";
    }
}