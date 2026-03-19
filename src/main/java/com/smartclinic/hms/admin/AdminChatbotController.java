package com.smartclinic.hms.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminChatbotController {

    @GetMapping("/chatbot")
    public String chatbot(Model model) {
        model.addAttribute("pageTitle", "AI 챗봇");
        model.addAttribute("isAdminChatbot", true);
        return "admin/chatbot";
    }
}
