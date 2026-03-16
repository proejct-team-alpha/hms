package com.smartclinic.hms.nurse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/nurse")
public class NurseChatbotController {

    @GetMapping("/chatbot")
    public String chatbot(Model model) {
        model.addAttribute("pageTitle", "AI 챗봇");
        return "nurse/chatbot";
    }
}
