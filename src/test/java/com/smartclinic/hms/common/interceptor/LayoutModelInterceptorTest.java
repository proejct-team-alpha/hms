package com.smartclinic.hms.common.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

class LayoutModelInterceptorTest {

    private final LayoutModelInterceptor interceptor = new LayoutModelInterceptor();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("postHandle marks admin patient menu active for admin patient paths")
    void postHandle_marksAdminPatientMenuActiveForAdminPatientPaths() throws Exception {
        // given
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/patient/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ModelAndView modelAndView = new ModelAndView("admin/patient-list");

        // when
        interceptor.postHandle(request, response, new Object(), modelAndView);

        // then
        assertThat(modelAndView.getModel().get("isAdminPatient")).isEqualTo(true);
        assertThat(modelAndView.getModel().get("isAdminReservation")).isEqualTo(false);
        assertThat(modelAndView.getModel().get("dashboardUrl")).isEqualTo("/admin/dashboard");
        assertThat(modelAndView.getModel().get("pageTitle")).isEqualTo("");
    }
}
