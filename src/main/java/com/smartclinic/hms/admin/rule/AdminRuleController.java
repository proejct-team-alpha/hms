package com.smartclinic.hms.admin.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rule")
public class AdminRuleController {

    private final HospitalRuleRepository hospitalRuleRepository;

    @GetMapping("/list")
    public String list(Model model) {
        List<AdminRuleDto> rules = hospitalRuleRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AdminRuleDto::new)
                .collect(Collectors.toList());
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
}
