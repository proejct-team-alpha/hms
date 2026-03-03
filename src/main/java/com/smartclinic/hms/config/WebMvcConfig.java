package com.smartclinic.hms.config;

import com.smartclinic.hms.common.interceptor.LayoutModelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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

    private final LayoutModelInterceptor layoutModelInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(layoutModelInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/css/**", "/js/**", "/images/**", "/favicon.ico",
                "/error", "/error/**",
                "/h2-console/**"
            );
    }
}
