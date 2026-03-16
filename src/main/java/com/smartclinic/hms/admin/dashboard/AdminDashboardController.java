package com.smartclinic.hms.admin.dashboard;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.smartclinic.hms.admin.dashboard.dto.AdminDashboardStatsResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardStatsService adminDashboardStatsService;

    @GetMapping("/dashboard")
    public String dashboard(HttpServletRequest req, Authentication authentication) {
        AdminDashboardStatsResponse stats = adminDashboardStatsService.getDashboardStats();

        req.setAttribute("model", stats);
        // 페이지 타이틀 설정정
        req.setAttribute("pageTitle", "관리자 대시보드");
        // 페이지 URL 설정
        req.setAttribute("dashboardUrl", "/admin/dashboard");
        // 사용자 이름름
        req.setAttribute("loginName", authentication == null ? "" : authentication.getName());
        // 사용자 권한 레이블 설정
        req.setAttribute("roleLabel", resolveRoleLabel(authentication));
        // 관리자 대시보드 여부 설정
        req.setAttribute("isAdminDashboard", true);
        return "admin/dashboard";
    }

    private String resolveRoleLabel(Authentication authentication) {
        if (authentication == null) {
            return "";
        }

        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .findFirst()
                .map(authority -> switch (authority) {
                    case "ROLE_ADMIN" -> "ADMIN";
                    case "ROLE_DOCTOR" -> "DOCTOR";
                    case "ROLE_NURSE" -> "NURSE";
                    case "ROLE_STAFF" -> "STAFF";
                    case "ROLE_ITEM_MANAGER" -> "ITEM_MANAGER";
                    default -> "USER";
                })
                .orElse("USER");
    }

}
