package com.smartclinic.hms.llm;

import com.smartclinic.hms.common.LlmWebMvcTestSecurityConfig;
import com.smartclinic.hms.common.util.SecurityUtils;
import com.smartclinic.hms.llm.controller.ChatController;
import com.smartclinic.hms.llm.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
@Import(LlmWebMvcTestSecurityConfig.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @MockitoBean
    SecurityUtils securityUtils;

    @Test
    @DisplayName("POST /llm/chatbot/query - 비인증 접근 시 /login 리다이렉트")
    void query_비인증_리다이렉트() throws Exception {
        mockMvc.perform(post("/llm/chatbot/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"당직\"}"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("POST /llm/chatbot/query - DOCTOR 인증 200")
    void query_인증_200() throws Exception {
        given(chatService.callRuleLlmApi(anyString())).willReturn(Mono.just("당직 규정 응답"));

        MvcResult result = mockMvc.perform(post("/llm/chatbot/query")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"당직\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /llm/chatbot/query/stream - DOCTOR 인증 SSE 200")
    void queryStream_인증_200() throws Exception {
        given(chatService.callRuleLlmApiStream(anyString())).willReturn(Flux.just("token1", "token2"));

        MvcResult result = mockMvc.perform(post("/llm/chatbot/query/stream")
                        .with(user("doctor").roles("DOCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"당직\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /llm/chatbot/history/{staffId} - DOCTOR 인증, 본인 staffId 일치 시 200")
    void history_인증_200() throws Exception {
        // given - securityUtils가 인증된 staffId 1L 반환 (path variable과 일치)
        given(securityUtils.resolveStaffId()).willReturn(1L);
        given(chatService.getRuleHistory(any(), any()))
                .willReturn(Page.empty());

        mockMvc.perform(get("/llm/chatbot/history/1")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /llm/chatbot/history/{staffId} - 타인의 staffId 접근 시 403")
    void history_타인staffId_403() throws Exception {
        // given - 인증된 사용자 staffId가 2L이지만 path variable은 1L
        given(securityUtils.resolveStaffId()).willReturn(2L);

        mockMvc.perform(get("/llm/chatbot/history/1")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }
}
