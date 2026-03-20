package com.smartclinic.hms.common.util;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SecurityUtils {

    private final StaffRepository staffRepository;

    /**
     * 현재 인증된 사용자의 staffId를 반환한다.
     * 비인증(익명) 사용자인 경우 null을 반환한다.
     */
    public Long resolveStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return staffRepository.findByUsernameAndActiveTrue(auth.getName())
                .map(Staff::getId).orElse(null);
    }
}
