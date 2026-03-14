package com.smartclinic.hms.staff.reception;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/staff/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;

    // 접수 목록
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "") String status, Model model) {
        model.addAttribute("reservations", receptionService.getTodayReservations(status));
        model.addAttribute("filters", receptionService.getStatusFilters(status));
        return "staff/reception-list";
    }

    // 접수 상세
    @GetMapping("/detail")
    public String detail(@RequestParam Long id, Model model) {
        model.addAttribute("detail", receptionService.getDetail(id));
        return "staff/reception-detail";
    }

    // 접수 처리
    @PostMapping("/receive")
    public String receive(@ModelAttribute ReceptionUpdateRequest request,
            RedirectAttributes redirectAttributes) {
        receptionService.receive(request);
        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");
        return "redirect:/staff/reception/list";
    }

    // 예약 취소
    @PostMapping("/cancel")
    public String cancel(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        receptionService.cancel(id);
        redirectAttributes.addFlashAttribute("message", "예약이 취소되었습니다.");
        return "redirect:/staff/reception/list";
    }
}
