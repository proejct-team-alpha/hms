package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff")
public class WalkinController {

    private final WalkinService walkinService;

    // 방문 접수 화면
    @GetMapping("/walkin-reception")
    public String walkinPage(Model model) {
        // 오늘 날짜
        model.addAttribute("today", LocalDate.now());

        // 현재 시간 (HH:mm 형식)
        String now = LocalTime.now()
                .withSecond(0)
                .withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        model.addAttribute("now", now);
        return "staff/walkin-reception";
    }

    // 방문 접수 생성
    @PostMapping("/walkin")
    public String createWalkin(WalkinRequestDto request) {

        walkinService.createWalkin(request);

        return "redirect:/staff/reception/list";
    }
}
