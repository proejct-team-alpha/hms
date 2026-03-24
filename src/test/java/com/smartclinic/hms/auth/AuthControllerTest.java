package com.smartclinic.hms.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    private final AuthController authController = new AuthController();

    @Test
    @DisplayName("deactivated 파라미터가 있으면 자동 로그아웃 안내 플래그를 모델에 담는다")
    void login_setsDeactivatedFlag() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Model model = new ExtendedModelMap();

        String viewName = authController.login(false, false, true, request, model);

        assertThat(viewName).isEqualTo("auth/login");
        assertThat(model.getAttribute("error")).isEqualTo(false);
        assertThat(model.getAttribute("logout")).isEqualTo(false);
        assertThat(model.getAttribute("deactivated")).isEqualTo(true);
    }
}
