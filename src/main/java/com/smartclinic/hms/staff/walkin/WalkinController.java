package com.smartclinic.hms.staff.walkin;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.staff.reception.ReceptionService;
import com.smartclinic.hms.staff.walkin.dto.WalkinRequestDto;
import com.smartclinic.hms.common.exception.CustomException;

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
        model.addAttribute("today", LocalDate.now().toString());
        return "staff/walkin-reception";
    }

    // 방문 접수 생성
    @PostMapping("/walkin")
    public String createWalkin(WalkinRequestDto request, RedirectAttributes redirectAttributes, Model model) {
        try {
            boolean nameMismatch = walkinService.createWalkin(request);

            if (nameMismatch) {
                redirectAttributes.addFlashAttribute("message",
                        "입력하신 이름과 기존 환자의 이름이 다릅니다. 기존 환자명으로 방문 접수가 완료되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("message", "방문 접수가 완료되었습니다.");
            }

            return "redirect:/staff/reception/list?date=" + request.getDate();

        } catch (CustomException e) {
            model.addAttribute("message", e.getMessage());
            model.addAttribute("form", request);
            model.addAttribute("departments", receptionService.getAllDepartments());
            model.addAttribute("doctors", receptionService.getAllDoctors());
            model.addAttribute("today", LocalDate.now().toString());
            return "staff/walkin-reception";
        }
    }
}
