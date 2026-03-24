package com.smartclinic.hms.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 에러 화면 — 403, 404.
 * API §1.6, documents 06_화면_기능_정의서.
 */
@Controller
@RequestMapping("/error")
public class ErrorPageController {

    @RequestMapping("/403")
    public String forbidden() {
        return "error/403";
    }

    @RequestMapping("/404")
    public String notFound() {
        return "error/404";
    }
}
