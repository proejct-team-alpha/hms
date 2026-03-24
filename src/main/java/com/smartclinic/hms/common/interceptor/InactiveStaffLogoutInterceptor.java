package com.smartclinic.hms.common.interceptor;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.Staff;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@RequiredArgsConstructor
public class InactiveStaffLogoutInterceptor implements HandlerInterceptor {

    private final StaffRepository staffRepository;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        return staffRepository.findByUsername(authentication.getName())
                .map(staff -> handleStaff(request, response, authentication, staff))
                .orElse(true);
    }

    private boolean handleStaff(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication,
            Staff staff) {
        if (staff.isActive()) {
            return true;
        }

        logoutHandler.logout(request, response, authentication);
        redirectToDeactivatedLogin(request, response);
        return false;
    }

    private void redirectToDeactivatedLogin(HttpServletRequest request, HttpServletResponse response) {
        String contextPath = request.getContextPath();
        String target = (contextPath == null ? "" : contextPath) + "/login?deactivated=true";
        try {
            response.sendRedirect(target);
        } catch (IOException ex) {
            throw new IllegalStateException("비활성 계정 리다이렉트에 실패했습니다.", ex);
        }
    }
}
