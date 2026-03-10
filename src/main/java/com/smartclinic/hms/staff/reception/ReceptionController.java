package com.smartclinic.hms.staff.reception;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.reservation.reservation.ReservationRepository;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/staff/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;
    private final ReservationRepository reservationRepository;

    // 접수 목록
    @GetMapping("/list")
    public String list(Model model) {

        List<Reservation> reservations = reservationRepository.findAll();
        model.addAttribute("reservations", reservations);

        return "staff/reception-list";
    }

    // 접수 처리 화면보기
    @GetMapping("/detail")
    public String detail() {
        return "staff/reception-detail";
    }

    // 접수 처리 하기
    @PostMapping("/receive")
    public String receive(@ModelAttribute ReceptionUpdateRequest request,
            RedirectAttributes redirectAttributes) {

        receptionService.receive(request);

        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");
        return "redirect:/staff/reception/list";

    }

}
