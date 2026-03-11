package com.smartclinic.hms.staff.walkin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/walkin")
public class WalkinController {

    private final WalkinService walkinService;

    /**
     * 방문 접수 화면
     * GET /staff/walkin
     */
    @GetMapping
    public String walkinPage() {
        return "staff/walkin-reception";
    }

    /**
     * 방문 접수 생성
     * POST /staff/walkin
     */
    @PostMapping
    public String createWalkin() {

        walkinService.createWalkin();

        return "redirect:/staff/reception/list";
    }
}
