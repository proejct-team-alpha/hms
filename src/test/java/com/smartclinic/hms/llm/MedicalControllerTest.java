package com.smartclinic.hms.llm;

import com.smartclinic.hms.common.LlmWebMvcTestSecurityConfig;
import com.smartclinic.hms.common.util.SecurityUtils;
import com.smartclinic.hms.domain.MedicalHistory;
import com.smartclinic.hms.llm.controller.MedicalController;
import com.smartclinic.hms.llm.service.DoctorService;
import com.smartclinic.hms.llm.service.LlmResponseParser;
import com.smartclinic.hms.llm.service.MedicalService;
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
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MedicalController.class)
@Import(LlmWebMvcTestSecurityConfig.class)
class MedicalControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MedicalService medicalService;

    @MockitoBean
    DoctorService doctorService;

    @MockitoBean
    LlmResponseParser llmResponseParser;

    @MockitoBean
    SecurityUtils securityUtils;

    @Test
    @DisplayName("POST /llm/medical/query - 비인증 200 (permitAll)")
    void query_비인증_200() throws Exception {
        MedicalHistory fakeHistory = new MedicalHistory("두통", "PENDING");
        given(medicalService.saveMedicalPending(anyString(), any())).willReturn(fakeHistory);
        given(medicalService.callMedicalLlmApi(anyString())).willReturn(Mono.just("두통 관련 LLM 응답"));
        MvcResult result = mockMvc.perform(post("/llm/medical/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"두통이 심해요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /llm/medical/query/consult - 비인증 200, recommendedDepartment 검증")
    void consultQuery_비인증_200() throws Exception {
        MedicalHistory fakeHistory = new MedicalHistory("소화불량", "PENDING");
        given(medicalService.saveMedicalPending(anyString(), any())).willReturn(fakeHistory);
        given(medicalService.callMedicalLlmApi(anyString())).willReturn(Mono.just("소화불량 상담 응답"));
        given(llmResponseParser.extractDepartment(anyString())).willReturn("내과");
        given(llmResponseParser.extractRecommendationReason(anyString())).willReturn("소화기 전문");
        given(doctorService.findDoctorsWithSchedule(anyString())).willReturn(List.of());

        MvcResult result = mockMvc.perform(post("/llm/medical/query/consult")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"소화불량이 있어요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedDepartment").value("내과"));
    }

    @Test
    @DisplayName("GET /llm/medical/history/{staffId} - 인증, 본인 staffId 일치 시 200")
    void history_인증_200() throws Exception {
        // given - securityUtils가 인증된 staffId 1L 반환 (path variable과 일치)
        given(securityUtils.resolveStaffId()).willReturn(1L);
        given(medicalService.getMedicalHistory(any(), any()))
                .willReturn(Page.empty());

        mockMvc.perform(get("/llm/medical/history/1")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /llm/medical/history/{staffId} - 타인의 staffId 접근 시 403")
    void history_타인staffId_403() throws Exception {
        // given - 인증된 사용자 staffId가 2L이지만 path variable은 1L
        given(securityUtils.resolveStaffId()).willReturn(2L);

        mockMvc.perform(get("/llm/medical/history/1")
                        .with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }
}
