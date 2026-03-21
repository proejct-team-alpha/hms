package com.smartclinic.hms.llm;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.MedicalHistory;
import com.smartclinic.hms.domain.MedicalHistoryRepository;
import com.smartclinic.hms.llm.service.MedicalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalService 단위 테스트")
class MedicalServiceTest {

    @Mock
    WebClient llmWebClient;

    @Mock
    MedicalHistoryRepository medicalHistoryRepository;

    @Mock
    StaffRepository staffRepository;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    MedicalService medicalService;

    @Test
    @DisplayName("updateMedicalCompleted — 메타데이터 JSON 직렬화 실패 시 IllegalStateException 전파")
    void updateMedicalCompleted_metadataJsonFails_propagatesIllegalState() throws Exception {
        // given
        MedicalHistory history = new MedicalHistory("두통", "PENDING");
        given(medicalHistoryRepository.findById(1L)).willReturn(Optional.of(history));
        doThrow(JacksonException.class).when(objectMapper).writeValueAsString(any());

        // when & then
        assertThatThrownBy(() -> medicalService.updateMedicalCompleted(1L, "답변", 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("메타데이터 JSON 직렬화");
    }
}
