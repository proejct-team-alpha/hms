package com.smartclinic.hms.item;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/item-manager")
public class ItemManagerChatbotController {

    @GetMapping("/chatbot")
    public String chatbot(Model model) {
        model.addAttribute("pageTitle", "AI 챗봇");
        model.addAttribute("isItemChatbot", true);
        return "item-manager/chatbot";
    }
}
