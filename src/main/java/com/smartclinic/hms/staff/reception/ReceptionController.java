package com.smartclinic.hms.staff.reception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.domain.Reservation;
import com.smartclinic.hms.domain.ReservationStatus;
import com.smartclinic.hms.staff.reception.dto.ReceptionUpdateRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/staff/reception")
@RequiredArgsConstructor
public class ReceptionController {

    private final ReceptionService receptionService;

    // 접수 목록
    @GetMapping("/list")
    public String list(Model model) {

        List<Reservation> reservations = receptionService.getReservations();

        List<Map<String, Object>> viewReservations = reservations.stream().map(r -> {

            Map<String, Object> map = new HashMap<>();

            map.put("id", r.getId());
            map.put("timeSlot", r.getTimeSlot());
            map.put("patient", r.getPatient());
            map.put("doctor", r.getDoctor());
            map.put("department", r.getDepartment());
            map.put("source", r.getSource());
            map.put("status", r.getStatus());

            String statusKor = switch (r.getStatus()) {
                case RESERVED -> "예약";
                case RECEIVED -> "접수완료";
                case CANCELLED -> "취소";
                case COMPLETED -> "진료완료";
            };

            map.put("statusKor", statusKor);

            map.put("showReceiveBtn", r.getStatus() == ReservationStatus.RESERVED);

            return map;

        }).toList();

        model.addAttribute("reservations", viewReservations);

        return "staff/reception-list";
    }

    // 접수 처리 화면보기
    @GetMapping("/detail")
    public String detail() {
        return "staff/reception-detail";
    }

    // 접수 처리 하기
    @PostMapping("/receive")
    public String receive(@Valid @ModelAttribute ReceptionUpdateRequest request,
            RedirectAttributes redirectAttributes) {

        receptionService.receive(request);

        redirectAttributes.addFlashAttribute("message", "접수가 완료되었습니다.");
        return "redirect:/staff/reception/list";

    }

}
