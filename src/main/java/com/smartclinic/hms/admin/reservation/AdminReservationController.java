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

import java.util.Set;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservation")
public class AdminReservationController {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ALL", "RESERVED", "RECEIVED", "COMPLETED", "CANCELLED");

    // 서비스 선언
    private final AdminReservationService adminReservationService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            HttpServletRequest req) {
        AdminReservationListResponse result = adminReservationService.getReservationList(page, size, status);
        req.setAttribute("model", result);
        req.setAttribute("pageTitle", "예약 목록");
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
            String successMessage = adminReservationService.cancelReservation(reservationId);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        // URL 파라미터 유지
        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        redirectAttributes.addAttribute("status", normalizeStatus(status));

        return "redirect:/admin/reservation/list";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ALL";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_STATUSES.contains(normalized) ? normalized : "ALL";
    }
}
