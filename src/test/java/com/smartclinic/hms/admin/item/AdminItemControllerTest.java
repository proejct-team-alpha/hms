package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.common.AdminControllerTestSecurityConfig;
import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLog;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        value = AdminItemController.class,
        properties = {
                "spring.mustache.servlet.expose-request-attributes=true",
                "spring.mustache.servlet.allow-request-override=true"
        }
)
@Import(AdminControllerTestSecurityConfig.class)
class AdminItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminItemService adminItemService;

    @MockitoBean
    private ItemManagerService itemManagerService;

    @Test
    @DisplayName("restock delegates to admin item service")
    void restock_delegatesToAdminItemService() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(post("/admin/item/restock")
                        .param("id", "1")
                        .param("amount", "5")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/item/list*"));

        then(adminItemService).should().restockItem(1L, 5);
        then(itemManagerService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("restock with invalid amount does not call services")
    void restock_withInvalidAmount_doesNotCallServices() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(post("/admin/item/restock")
                        .param("id", "1")
                        .param("amount", "abc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/item/list*"));

        then(adminItemService).shouldHaveNoInteractions();
        then(itemManagerService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("history uses today as default date range")
    void history_usesTodayAsDefaultDateRange() throws Exception {
        // given
        LocalDate today = LocalDate.now();
        given(itemManagerService.getStockHistory(today, today)).willReturn(List.of());
        given(itemManagerService.getTotalInAmount(today, today)).willReturn(0L);
        given(itemManagerService.getTotalOutAmount(today, today)).willReturn(0L);

        // when
        // then
        mockMvc.perform(get("/admin/item/history")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item-history"))
                .andExpect(model().attribute("fromDate", today.toString()))
                .andExpect(model().attribute("toDate", today.toString()))
                .andExpect(content().string(containsString("history-filter-form")))
                .andExpect(content().string(containsString("history-from-date")))
                .andExpect(content().string(containsString("history-to-date")))
                .andExpect(content().string(containsString("history-search")))
                .andExpect(content().string(containsString("history-type")))
                .andExpect(model().attributeDoesNotExist("dateError"));

        then(itemManagerService).should().getStockHistory(today, today);
        then(itemManagerService).should().getTotalInAmount(today, today);
        then(itemManagerService).should().getTotalOutAmount(today, today);
    }

    @Test
    @DisplayName("history passes requested date range to service")
    void history_passesDateRangeParamsToService() throws Exception {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        given(itemManagerService.getStockHistory(fromDate, toDate)).willReturn(List.of());
        given(itemManagerService.getTotalInAmount(fromDate, toDate)).willReturn(15L);
        given(itemManagerService.getTotalOutAmount(fromDate, toDate)).willReturn(8L);

        // when
        // then
        mockMvc.perform(get("/admin/item/history")
                        .param("fromDate", "2026-03-20")
                        .param("toDate", "2026-03-21")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-20"))
                .andExpect(model().attribute("toDate", "2026-03-21"))
                .andExpect(model().attributeDoesNotExist("dateError"));

        then(itemManagerService).should().getStockHistory(fromDate, toDate);
        then(itemManagerService).should().getTotalInAmount(fromDate, toDate);
        then(itemManagerService).should().getTotalOutAmount(fromDate, toDate);
    }

    @Test
    @DisplayName("history renders validation error when only one date is provided")
    void history_withOnlyOneDate_rendersValidationError() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/admin/item/history")
                        .param("fromDate", "2026-03-20")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-20"))
                .andExpect(model().attribute("toDate", ""))
                .andExpect(model().attributeExists("dateError"));

        then(itemManagerService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("history renders validation error when date range is reversed")
    void history_withReversedDate_rendersValidationError() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/admin/item/history")
                        .param("fromDate", "2026-03-22")
                        .param("toDate", "2026-03-21")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-22"))
                .andExpect(model().attribute("toDate", "2026-03-21"))
                .andExpect(model().attributeExists("dateError"));

        then(itemManagerService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("history renders validation error when date format is invalid")
    void history_withInvalidDateFormat_rendersValidationError() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(get("/admin/item/history")
                        .param("fromDate", "2026-03-XX")
                        .param("toDate", "2026-03-21")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item-history"))
                .andExpect(model().attribute("fromDate", "2026-03-XX"))
                .andExpect(model().attribute("toDate", "2026-03-21"))
                .andExpect(model().attributeExists("dateError"));

        then(itemManagerService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("use page renders today-only usage dashboard")
    void usePage_rendersTodayOnlyUsageDashboard() throws Exception {
        // given
        given(itemManagerService.getItemList(null)).willReturn(List.of());
        given(itemManagerService.getTodayStaffUsageLogs()).willReturn(List.of());
        given(itemManagerService.getTodayTotalStaffUsageAmount()).willReturn(0L);

        // when
        // then
        mockMvc.perform(get("/admin/item/use")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/item-use"))
                .andExpect(model().attribute("todayLogCount", 0))
                .andExpect(model().attribute("todayTotalUsedAmount", 0L))
                .andExpect(content().string(containsString("today-log-tbody")))
                .andExpect(content().string(containsString("item-category-btns")))
                .andExpect(content().string(containsString("item-search")))
                .andExpect(content().string(containsString("card-grid-pagination")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("use-date-form"))));

        then(itemManagerService).should().getItemList(null);
        then(itemManagerService).should().getTodayStaffUsageLogs();
        then(itemManagerService).should().getTodayTotalStaffUsageAmount();
    }

    @Test
    @DisplayName("use api returns refreshed today logs and totals")
    void useApi_returnsRefreshedTodayLogsAndTotals() throws Exception {
        // given
        ItemUsageLog log = ItemUsageLog.of(null, 1L, "Syringe", 3, "관리자");
        ReflectionTestUtils.setField(log, "usedAt", LocalDateTime.of(2026, 3, 24, 10, 15));
        ItemUsageLogDto dto = new ItemUsageLogDto(log);
        given(itemManagerService.useItem(1L, 3, null)).willReturn(7);
        given(itemManagerService.getTodayStaffUsageLogs()).willReturn(List.of(dto));
        given(itemManagerService.getTodayTotalStaffUsageAmount()).willReturn(3L);

        // when
        // then
        mockMvc.perform(post("/admin/item/use")
                        .param("id", "1")
                        .param("amount", "3")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7))
                .andExpect(jsonPath("$.todayLogCount").value(1))
                .andExpect(jsonPath("$.todayTotalUsedAmount").value(3))
                .andExpect(jsonPath("$.logs[0].itemName").value("Syringe"))
                .andExpect(jsonPath("$.logs[0].amount").value(3))
                .andExpect(jsonPath("$.logs[0].usedAt").value("10:15"));

        then(itemManagerService).should().useItem(1L, 3, null);
        then(itemManagerService).should().getTodayStaffUsageLogs();
        then(itemManagerService).should().getTodayTotalStaffUsageAmount();
    }

    @Test
    @DisplayName("use api returns bad request when amount is invalid")
    void useApi_withInvalidAmount_returnsBadRequest() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(post("/admin/item/use")
                        .param("id", "1")
                        .param("amount", "abc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        then(itemManagerService).shouldHaveNoInteractions();
    }
}