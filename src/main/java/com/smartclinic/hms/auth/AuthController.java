package com.smartclinic.hms.auth;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 인증 화면 (API §2.1)
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) Boolean error,
                        @RequestParam(value = "logout", required = false) Boolean logout,
                        Model model) {
        model.addAttribute("error", Boolean.TRUE.equals(error));
        model.addAttribute("logout", Boolean.TRUE.equals(logout));
        // _csrf는 Spring Security가 request attribute로 노출하므로 뷰에서 자동 사용 가능
        return "auth/login";
    }
}
