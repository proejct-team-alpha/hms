package com.smartclinic.hms.staff;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffChatbotController {

    @GetMapping("/chatbot")
    public String chatbot(Model model) {
        model.addAttribute("pageTitle", "AI 챗봇");
        model.addAttribute("isStaffChatbot", true);
        return "staff/chatbot";
    }
}
