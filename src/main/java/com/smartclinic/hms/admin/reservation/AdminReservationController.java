package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservation")
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            Model model
    ) {
        AdminReservationListResponse result = adminReservationService.getReservationList(page, size, status);
        model.addAttribute("data", result);
        model.addAttribute("pageTitle", "전체 예약 목록");
        return "admin/reservation-list";
    }
}
