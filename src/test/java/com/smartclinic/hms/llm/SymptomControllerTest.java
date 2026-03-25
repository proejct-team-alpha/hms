package com.smartclinic.hms.llm;

import com.smartclinic.hms.common.LlmWebMvcTestSecurityConfig;
import com.smartclinic.hms.llm.controller.SymptomController;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import com.smartclinic.hms.llm.service.SymptomAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SymptomController.class)
@Import(LlmWebMvcTestSecurityConfig.class)
class SymptomControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SymptomAnalysisService symptomAnalysisService;

    @Test
    @DisplayName("POST /llm/symptom/analyze - 비인증 200 (permitAll)")
    void analyze_비인증_200() throws Exception {
        given(symptomAnalysisService.analyzeSymptom(anyString()))
                .willReturn(Mono.just(new SymptomResponse(1L, "내과", null)));

        MvcResult result = mockMvc.perform(post("/llm/symptom/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptomText\":\"열이 나요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /llm/symptom/analyze - 응답 구조 확인 (departmentId, departmentName)")
    void analyze_응답구조() throws Exception {
        given(symptomAnalysisService.analyzeSymptom(anyString()))
                .willReturn(Mono.just(new SymptomResponse(3L, "소아과", "아이의 발열 증상은 소아과 진료가 적합합니다.")));

        MvcResult result = mockMvc.perform(post("/llm/symptom/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symptomText\":\"아이가 열이 나요\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(3))
                .andExpect(jsonPath("$.departmentName").value("소아과"));
    }
}
