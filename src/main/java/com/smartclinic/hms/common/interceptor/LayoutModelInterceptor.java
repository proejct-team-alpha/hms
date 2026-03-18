package com.smartclinic.hms.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 공통 레이아웃 모델 자동 주입 (documents §2, 아키텍처 §5.2)
 *
 * 주입 키: pageTitle, loginName, roleLabel, isAdmin, isDoctor, isNurse, isStaff,
 *         showChatbot, currentPath, dashboardUrl, _csrf,
 *         sidebar 활성화 플래그 (isStaffDashboard, isAdminDashboard 등)
 */
@Component
public class LayoutModelInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        if (modelAndView == null) return;

        if (!modelAndView.getModel().containsKey("pageTitle")) {
            modelAndView.addObject("pageTitle", "");
        }
        String currentPath = request.getRequestURI();
        modelAndView.addObject("currentPath", currentPath);

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            modelAndView.addObject("_csrf", csrfToken);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String loginName = auth.getName();
            boolean isAdmin = hasRole(auth, "ADMIN");
            boolean isDoctor = hasRole(auth, "DOCTOR");
            boolean isNurse = hasRole(auth, "NURSE");
            boolean isStaff = hasRole(auth, "STAFF");
            boolean isItemManager = hasRole(auth, "ITEM_MANAGER");

            modelAndView.addObject("loginName", loginName);
            modelAndView.addObject("roleLabel", resolveRoleLabel(auth));
            modelAndView.addObject("isAdmin", isAdmin);
            modelAndView.addObject("isDoctor", isDoctor);
            modelAndView.addObject("isNurse", isNurse);
            modelAndView.addObject("isStaff", isStaff);
            modelAndView.addObject("showChatbot", isDoctor || isNurse);
            modelAndView.addObject("dashboardUrl", resolveDashboardUrl(auth));

            addSidebarActiveFlags(modelAndView, currentPath);
        } else {
            modelAndView.addObject("loginName", null);
            modelAndView.addObject("roleLabel", "");
            modelAndView.addObject("isAdmin", false);
            modelAndView.addObject("isDoctor", false);
            modelAndView.addObject("isNurse", false);
            modelAndView.addObject("isStaff", false);
            modelAndView.addObject("showChatbot", false);
            modelAndView.addObject("dashboardUrl", "/");
        }
    }

    private void addSidebarActiveFlags(ModelAndView mav, String path) {
        mav.addObject("isStaffDashboard", path.equals("/staff/dashboard"));
        mav.addObject("isStaffReception", path.startsWith("/staff/reception") || path.startsWith("/staff/reception-list"));
        mav.addObject("isStaffPhone", path.contains("/staff/phone") || path.contains("phone-reservation"));
        mav.addObject("isStaffWalkin", path.contains("/staff/walkin") || path.contains("walkin-reception"));
        mav.addObject("isStaffMypage", path.startsWith("/staff/mypage"));
        mav.addObject("isStaffItemUse", path.startsWith("/staff/item"));

        mav.addObject("isDoctorDashboard", path.equals("/doctor/dashboard"));
        mav.addObject("isDoctorTreatment", path.startsWith("/doctor/treatment") || path.contains("treatment"));
        mav.addObject("isDoctorCompleted", path.contains("/doctor/completed") || path.contains("completed-list"));
        mav.addObject("isDoctorChatbot", path.startsWith("/doctor/chatbot"));
        mav.addObject("isDoctorMypage", path.startsWith("/doctor/mypage"));

        mav.addObject("isNurseDashboard", path.equals("/nurse/dashboard"));
        mav.addObject("isNurseReception", path.startsWith("/nurse/reception") || path.contains("reception-list") || path.startsWith("/nurse/patient-detail"));
        mav.addObject("isNurseChatbot", path.startsWith("/nurse/chatbot"));
        mav.addObject("isNurseMypage", path.startsWith("/nurse/mypage"));

        mav.addObject("isAdminDashboard", path.equals("/admin/dashboard"));
        mav.addObject("isAdminReservation", path.startsWith("/admin/reservation"));
        mav.addObject("isAdminDepartment", path.startsWith("/admin/department"));
        mav.addObject("isAdminRule", path.startsWith("/admin/rule"));
        mav.addObject("isAdminStaff", path.startsWith("/admin/staff"));
        mav.addObject("isAdminItem", path.startsWith("/item-manager"));

        mav.addObject("isItemDashboard", path.equals("/item-manager/dashboard"));
        mav.addObject("isItemList", path.startsWith("/item-manager/item-list"));
        mav.addObject("isItemForm", path.startsWith("/item-manager/item-form"));
        mav.addObject("isItemUse", path.startsWith("/item-manager/item-use"));
        mav.addObject("isItemHistory", path.startsWith("/item-manager/item-history"));
        mav.addObject("isItemMypage", path.startsWith("/item-manager/mypage"));
    }

    private static String resolveRoleLabel(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            return switch (a.getAuthority()) {
                case "ROLE_ADMIN" -> "ADMIN";
                case "ROLE_DOCTOR" -> "DOCTOR";
                case "ROLE_NURSE" -> "NURSE";
                case "ROLE_STAFF" -> "STAFF";
                case "ROLE_ITEM_MANAGER" -> "ITEM_MANAGER";
                default -> "USER";
            };
        }
        return "USER";
    }

    private static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_" + role));
    }

    private static String resolveDashboardUrl(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            return switch (a.getAuthority()) {
                case "ROLE_ADMIN" -> "/admin/dashboard";
                case "ROLE_DOCTOR" -> "/doctor/dashboard";
                case "ROLE_NURSE" -> "/nurse/dashboard";
                case "ROLE_STAFF" -> "/staff/dashboard";
                case "ROLE_ITEM_MANAGER" -> "/item-manager/dashboard";
                default -> "/";
            };
        }
        return "/";
    }
}
