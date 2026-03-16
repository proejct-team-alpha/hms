package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;
<<<<<<< HEAD
=======
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
>>>>>>> dev

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;

import jakarta.validation.Valid;
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
<<<<<<< HEAD
        model.addAttribute("departments", receptionService.getAllDepartments());
        model.addAttribute("doctors", receptionService.getAllDoctors());
        model.addAttribute("today", LocalDate.now().toString());
=======
        // 오늘 날짜
        model.addAttribute("today", LocalDate.now());

        // 현재 시간 (HH:mm 형식)
        String now = LocalTime.now()
                .withSecond(0)
                .withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        model.addAttribute("now", now);
>>>>>>> dev
        return "staff/walkin-reception";
    }

    // 방문 접수 생성
    @PostMapping("/walkin")
<<<<<<< HEAD
    public String createWalkin(WalkinRequestDto request, RedirectAttributes redirectAttributes) {
=======
    public String createWalkin(@Valid WalkinRequestDto request) {

>>>>>>> dev
        walkinService.createWalkin(request);
        redirectAttributes.addFlashAttribute("message", "방문 접수가 완료되었습니다.");
        return "redirect:/staff/reception/list?date=" + request.getDate();
    }
}
