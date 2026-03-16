package com.smartclinic.hms.nurse.mypage;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/nurse")
public class NurseMypageController {

    private final NurseMypageService nurseMypageService;

    @GetMapping("/mypage")
    public String mypage(Authentication auth, Model model) {
        model.addAttribute("info", nurseMypageService.getMypage(auth.getName()));
        model.addAttribute("pageTitle", "내 정보 관리");
        return "nurse/mypage";
    }
}
