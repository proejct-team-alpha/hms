package com.smartclinic.hms.config;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.common.interceptor.LayoutModelInterceptor;
import com.smartclinic.hms.common.interceptor.InactiveStaffLogoutInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * Spring MVC 설정 — Interceptor 등록
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ LayoutModelInterceptor: pageTitle, loginName, isAdmin/isDoctor/isNurse/isStaff,
 *   showChatbot, currentPath, dashboardUrl 자동 주입
 * ■ 제외 경로: /css, /js, /images, /error, /h2-console (documents §2)
 * ════════════════════════════════════════════════════════════════════════════
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ObjectProvider<InactiveStaffLogoutInterceptor> inactiveStaffLogoutInterceptorProvider;
    private final LayoutModelInterceptor layoutModelInterceptor;

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @ConditionalOnBean(StaffRepository.class)
    InactiveStaffLogoutInterceptor inactiveStaffLogoutInterceptor(StaffRepository staffRepository) {
        return new InactiveStaffLogoutInterceptor(staffRepository);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        InactiveStaffLogoutInterceptor inactiveStaffLogoutInterceptor = inactiveStaffLogoutInterceptorProvider.getIfAvailable();
        if (inactiveStaffLogoutInterceptor != null) {
            registry.addInterceptor(inactiveStaffLogoutInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/css/**", "/js/**", "/images/**", "/favicon.ico",
                    "/login", "/logout",
                    "/error", "/error/**",
                    "/h2-console/**"
                );
        }

        registry.addInterceptor(layoutModelInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                "/login", "/logout",
                "/error", "/error/**",
                "/h2-console/**"
            );
    }
}
