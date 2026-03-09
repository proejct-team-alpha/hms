package com.smartclinic.hms.staff.reception;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff/reception")
public class ReceptionController {

    @GetMapping("/list")
    public String list() {
        return "staff/reception-list";
    }
}
