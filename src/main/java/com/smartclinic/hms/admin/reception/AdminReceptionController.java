package com.smartclinic.hms.admin.reception;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/reception")
public class AdminReceptionController {

    @GetMapping("/list")
    public String list() {
        // 1차는 reservation 화면 재사용, reception 데이터 로직은 2차에서 분리
        return "admin/reservation-list";
    }
}

