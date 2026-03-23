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
import java.util.Set;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/reservation")
public class AdminReservationController {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ALL", "RESERVED", "RECEIVED", "COMPLETED", "CANCELLED");

    private final AdminReservationService adminReservationService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpServletRequest req) {
        AdminReservationListResponse result = adminReservationService.getReservationList(page, size, status, keyword);
        req.setAttribute("model", result);
        req.setAttribute("pageTitle", "예약 목록");
        return "admin/reservation-list";
    }

    @PostMapping("/cancel")
    public String cancel(
            @RequestParam(name = "reservationId") Long reservationId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", defaultValue = "ALL") String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            RedirectAttributes redirectAttributes) {

        try {
            String successMessage = adminReservationService.cancelReservation(reservationId);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        redirectAttributes.addAttribute("page", page);
        redirectAttributes.addAttribute("size", size);
        redirectAttributes.addAttribute("status", normalizeStatus(status));
        redirectAttributes.addAttribute("keyword", normalizeKeyword(keyword));

        return "redirect:/admin/reservation/list";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "ALL";
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return ALLOWED_STATUSES.contains(normalized) ? normalized : "ALL";
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}