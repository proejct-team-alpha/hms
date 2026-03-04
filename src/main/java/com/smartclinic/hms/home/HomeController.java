package com.smartclinic.hms.home;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Smart Clinic에 오신 것을 환영합니다.");
        return "home/index";
    }
}
