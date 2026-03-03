package com.smartclinic.hms.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 공통 레이아웃 모델 자동 주입 (documents §2, 아키텍처 §5.2)
 *
 * 주입 키: pageTitle, loginName, isAdmin, isDoctor, isNurse, isStaff,
 *         showChatbot, currentPath, dashboardUrl
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
        modelAndView.addObject("currentPath", request.getRequestURI());
        // _csrf는 Spring Security가 request attribute로 노출하므로 뷰에서 자동 사용 가능

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            String loginName = auth.getName();
            boolean isAdmin = hasRole(auth, "ADMIN");
            boolean isDoctor = hasRole(auth, "DOCTOR");
            boolean isNurse = hasRole(auth, "NURSE");
            boolean isStaff = hasRole(auth, "STAFF");

            modelAndView.addObject("loginName", loginName);
            modelAndView.addObject("isAdmin", isAdmin);
            modelAndView.addObject("isDoctor", isDoctor);
            modelAndView.addObject("isNurse", isNurse);
            modelAndView.addObject("isStaff", isStaff);
            modelAndView.addObject("showChatbot", isDoctor || isNurse);
            modelAndView.addObject("dashboardUrl", resolveDashboardUrl(auth));
        } else {
            modelAndView.addObject("loginName", null);
            modelAndView.addObject("isAdmin", false);
            modelAndView.addObject("isDoctor", false);
            modelAndView.addObject("isNurse", false);
            modelAndView.addObject("isStaff", false);
            modelAndView.addObject("showChatbot", false);
            modelAndView.addObject("dashboardUrl", "/");
        }
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
                default -> "/";
            };
        }
        return "/";
    }
}
