package com.smartclinic.hms.common.interceptor;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.domain.StaffRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InactiveStaffLogoutInterceptorTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private InactiveStaffLogoutInterceptor interceptor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("비활성 직원의 다음 요청은 자동 로그아웃 후 로그인 페이지로 이동한다")
    void preHandle_redirectsInactiveStaffToLogin() throws Exception {
        Staff inactiveStaff = Staff.create("admin01", "A-001", "{noop}pw", "관리자", StaffRole.ADMIN, null);
        inactiveStaff.update("관리자", null, false);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin01",
                "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        given(staffRepository.findByUsername("admin01")).willReturn(Optional.of(inactiveStaff));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        assertThat(response.getRedirectedUrl()).isEqualTo("/login?deactivated=true");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("활성 직원 요청은 그대로 통과한다")
    void preHandle_allowsActiveStaff() throws Exception {
        Staff activeStaff = Staff.create("admin01", "A-001", "{noop}pw", "관리자", StaffRole.ADMIN, null);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin01",
                "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
        given(staffRepository.findByUsername("admin01")).willReturn(Optional.of(activeStaff));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(response.getRedirectedUrl()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }
}
