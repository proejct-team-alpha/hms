package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservation")
public class AdminReservationController {

    private static final String RESERVATION_LIST_TITLE = "예약 목록";

    private final AdminReservationService adminReservationService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        AdminReservationListResponse result = adminReservationService.getReservationList(page, size, status, keyword);
        request.setAttribute("model", result);
        request.setAttribute("pageTitle", RESERVATION_LIST_TITLE);
        return "admin/reservation-list";
    }
}
