package com.smartclinic.hms.llm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/llm")
public class LlmPageController {

    @GetMapping("/medical")
    public String medicalPage() {
        return "llm/medical";
    }

    @GetMapping("/chatbot")
    public String chatbotPage() {
        return "llm/chatbot";
    }
}
