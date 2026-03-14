package com.smartclinic.hms.staff.walkin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff")
public class WalkinController {

    private final WalkinService walkinService;
    private final ReceptionService receptionService;

    // 방문 접수 화면
    @GetMapping("/walkin-reception")
    public String walkinPage(Model model) {
        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors());
        return "staff/walkin-reception";
    }

    // 방문 접수 생성
    @PostMapping("/walkin")
    public String createWalkin(WalkinRequestDto request) {

        walkinService.createWalkin(request);

        return "redirect:/staff/reception/list";
    }
}
