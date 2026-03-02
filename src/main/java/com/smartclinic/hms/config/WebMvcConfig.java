package com.smartclinic.hms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * Spring MVC 설정 — Interceptor 등록
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 인터셉터 등록 위치
 * common/interceptor/ 패키지에 구현된 인터셉터를 addInterceptors()에 등록.
 *
 * ■ 등록 예시 (인터셉터 구현 후 아래 패턴으로 추가)
 * - 전체 경로 적용 : addPathPatterns("/**")
 * - 특정 경로 제외 : excludePathPatterns("/login", "/reservation/**", ...)
 * - 순서 지정 : registry.addInterceptor(...).order(1)
 * ════════════════════════════════════════════════════════════════════════════
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * Interceptor 등록.
     * common/interceptor/ 에 인터셉터 구현 후 여기에 등록.
     *
     * <pre>
     * 등록 예시:
     *   registry.addInterceptor(loginCheckInterceptor)
     *           .addPathPatterns("/**")
     *           .excludePathPatterns(
     *               "/", "/login", "/logout",
     *               "/reservation/**",
     *               "/llm/symptom/**",
     *               "/css/**", "/js/**", "/images/**", "/error/**"
     *           );
     * </pre>
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // TODO: common/interceptor/ 구현 후 인터셉터 등록
    }
}
