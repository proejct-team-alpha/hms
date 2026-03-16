package com.smartclinic.hms.admin.reservation;

import com.smartclinic.hms.admin.reservation.dto.AdminReservationListResponse;
import com.smartclinic.hms.common.exception.CustomException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservation")
public class AdminReservationController {

    // 예약 취소 성공 메시지
    private static final String CANCEL_SUCCESS_MESSAGE = "예약이 취소되었습니다.";

    // 서비스 선언
    private final AdminReservationService adminReservationService;

    @GetMapping("/list")
    public String list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            HttpServletRequest req) {
        AdminReservationListResponse result = adminReservationService.getReservationList(page, size, status);
        req.setAttribute("model", result);
        return "admin/reservation-list";
    }

    @PostMapping("/cancel")
    public String cancel(
            @RequestParam Long reservationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ALL") String status,
            RedirectAttributes redirectAttributes) {

        try {
            adminReservationService.cancelReservation(reservationId);
            redirectAttributes.addFlashAttribute("successMessage", CANCEL_SUCCESS_MESSAGE);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // URL 파라미터 유지
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        redirectAttributes.addAttribute("status", status.toUpperCase(Locale.ROOT));

        return "redirect:/admin/reservation/list";
    }
}