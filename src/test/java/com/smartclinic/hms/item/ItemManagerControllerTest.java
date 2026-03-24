package com.smartclinic.hms.item;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemManagerController.class)
@Import(ItemManagerControllerTest.TestSecurityConfig.class)
class ItemManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemManagerService itemService;

    // ── GET /item-manager/item-list ──────────────────────────────────────────

    @Test
    @DisplayName("ROLE_ITEM_MANAGER — 물품 목록 페이지 접근 시 200 반환")
    void itemList_withItemManagerRole_returnsOk() throws Exception {
        given(itemService.getItemList(null)).willReturn(List.of());
        given(itemService.getCategoryFilters(null)).willReturn(List.of());

        mockMvc.perform(get("/item-manager/item-list")
                        .with(user("item01").roles("ITEM_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("미인증 — 물품 목록 페이지 접근 시 로그인 페이지로 리다이렉트")
    void itemList_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/item-manager/item-list"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("ROLE_STAFF — 물품 목록 페이지 접근 시 403 반환")
    void itemList_withStaffRole_isForbidden() throws Exception {
        mockMvc.perform(get("/item-manager/item-list")
                        .with(user("staff01").roles("STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("item history uses today as default date range")
    void itemHistory_usesTodayAsDefaultDateRange() throws Exception {
        // given
        LocalDate today = LocalDate.now();
        given(itemService.getStockHistory(today, today)).willReturn(List.of());
        given(itemService.getTotalInAmount(today, today)).willReturn(0L);
        given(itemService.getTotalOutAmount(today, today)).willReturn(0L);

        // when
        // then
        mockMvc.perform(get("/item-manager/item-history")
                        .with(user("item01").roles("ITEM_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("item-manager/item-history"))
                .andExpect(model().attribute("fromDate", today.toString()))
                .andExpect(model().attribute("toDate", today.toString()))
                .andExpect(model().attributeDoesNotExist("dateError"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("history-filter-form")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("history-from-date")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("history-to-date")));

        then(itemService).should().getStockHistory(today, today);
        then(itemService).should().getTotalInAmount(today, today);
        then(itemService).should().getTotalOutAmount(today, today);
    }

    @Test
    @DisplayName("item history passes requested date range to service")
    void itemHistory_passesDateRangeParamsToService() throws Exception {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        given(itemService.getStockHistory(fromDate, toDate)).willReturn(List.of());
        given(itemService.getTotalInAmount(fromDate, toDate)).willReturn(12L);
        given(itemService.getTotalOutAmount(fromDate, toDate)).willReturn(4L);

        // when
        // then
        mockMvc.perform(get("/item-manager/item-history")
                        .param("fromDate", "2026-03-20")
                        .param("toDate", "2026-03-21")
                        .with(user("item01").roles("ITEM_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("item-manager/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-20"))
                .andExpect(model().attribute("toDate", "2026-03-21"))
                .andExpect(model().attributeDoesNotExist("dateError"));

        then(itemService).should().getStockHistory(fromDate, toDate);
        then(itemService).should().getTotalInAmount(fromDate, toDate);
        then(itemService).should().getTotalOutAmount(fromDate, toDate);
    }

    @Test
    @DisplayName("item history renders validation error when only one date is provided")
    void itemHistory_withOnlyOneDate_rendersValidationError() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/item-manager/item-history")
                        .param("fromDate", "2026-03-20")
                        .with(user("item01").roles("ITEM_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("item-manager/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-20"))
                .andExpect(model().attribute("toDate", ""))
                .andExpect(model().attributeExists("dateError"));

        then(itemService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("item history renders validation error when date range is reversed")
    void itemHistory_withReversedDate_rendersValidationError() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/item-manager/item-history")
                        .param("fromDate", "2026-03-22")
                        .param("toDate", "2026-03-21")
                        .with(user("item01").roles("ITEM_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(view().name("item-manager/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-22"))
                .andExpect(model().attribute("toDate", "2026-03-21"))
                .andExpect(model().attributeExists("dateError"));

        then(itemService).shouldHaveNoInteractions();
    }

    // ── POST /item-manager/item-use ──────────────────────────────────────────

    @Test
    @DisplayName("ROLE_ITEM_MANAGER — 유효한 수량 출고 요청 시 200과 JSON 반환")
    void useItem_withValidAmount_returnsOkWithJson() throws Exception {
        given(itemService.useItem(anyLong(), any(Integer.class), any())).willReturn(40);
        given(itemService.getTodayStaffUsageLogs()).willReturn(List.of());

        mockMvc.perform(post("/item-manager/item-use")
                        .param("id", "1")
                        .param("amount", "10")
                        .with(user("item01").roles("ITEM_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(40));
    }

    @Test
    @DisplayName("ROLE_ITEM_MANAGER — 수량 0 출고 요청 시 400 반환")
    void useItem_withZeroAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/item-manager/item-use")
                        .param("id", "1")
                        .param("amount", "0")
                        .with(user("item01").roles("ITEM_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("ROLE_ITEM_MANAGER — 음수 수량 출고 요청 시 400 반환")
    void useItem_withNegativeAmount_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/item-manager/item-use")
                        .param("id", "1")
                        .param("amount", "-5")
                        .with(user("item01").roles("ITEM_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── TestSecurityConfig ───────────────────────────────────────────────────

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/login").permitAll()
                            .requestMatchers("/item-manager/**").hasRole("ITEM_MANAGER")
                            .anyRequest().authenticated())
                    .formLogin(form -> form
                            .loginPage("/login")
                            .permitAll())
                    .build();
        }

        @Bean
        UserDetailsService userDetailsService() {
            return new InMemoryUserDetailsManager(
                    User.withUsername("item01").password("{noop}password").roles("ITEM_MANAGER").build(),
                    User.withUsername("staff01").password("{noop}password").roles("STAFF").build());
        }
    }
}
