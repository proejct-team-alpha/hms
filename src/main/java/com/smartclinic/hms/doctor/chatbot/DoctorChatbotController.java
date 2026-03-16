package com.smartclinic.hms.doctor.chatbot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/doctor")
public class DoctorChatbotController {

    @GetMapping("/chatbot")
    public String chatbot(Model model) {
        model.addAttribute("pageTitle", "AI 챗봇");
        return "doctor/chatbot";
    }
}
