package com.smartclinic.hms.staff.reception;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/reception")
public class ReceptionController {

    // 접수 목록
    @GetMapping("/list")
    public String list() {
        return "staff/reception-list";
    }

    // 접수 처리
    @GetMapping("/detail")
    public String detail() {
        return "staff/reception-detail";
    }
}
