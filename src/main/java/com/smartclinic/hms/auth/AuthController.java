package com.smartclinic.hms.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.csrf.CsrfToken;
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
                        HttpServletRequest request,
                        Model model) {
        model.addAttribute("error", Boolean.TRUE.equals(error));
        model.addAttribute("logout", Boolean.TRUE.equals(logout));
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
        return "auth/login";
    }
}
