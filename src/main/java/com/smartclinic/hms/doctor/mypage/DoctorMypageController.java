package com.smartclinic.hms.doctor.mypage;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctor")
public class DoctorMypageController {

    private final DoctorMypageService doctorMypageService;

    @GetMapping("/mypage")
    public String mypage(Authentication auth, Model model) {
        model.addAttribute("info", doctorMypageService.getMypage(auth.getName()));
        model.addAttribute("pageTitle", "내 정보 관리");
        return "doctor/mypage";
    }
}
