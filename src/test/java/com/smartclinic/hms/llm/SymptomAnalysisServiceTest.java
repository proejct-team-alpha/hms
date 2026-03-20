package com.smartclinic.hms.llm;

import com.smartclinic.hms.common.exception.LlmServiceUnavailableException;
import com.smartclinic.hms.llm.dto.LlmResponse;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import com.smartclinic.hms.llm.service.SymptomAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("증상 분석 서비스 단위 테스트")
class SymptomAnalysisServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    WebClient llmWebClient;

    @InjectMocks
    SymptomAnalysisService symptomAnalysisService;

    @Test
    @DisplayName("정상 LLM 응답 파싱 성공 - 진료과/전문의/시간 추출")
    void analyzeSymptom_정상응답_파싱성공() {
        // given
        String llmText = "진료과: 내과\n전문의: 김내과의사\n시간: 09:30";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("두통이 있어요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDept()).isEqualTo("내과");
        assertThat(response.getDoctor()).isEqualTo("김내과의사");
        assertThat(response.getTime()).isEqualTo("09:30");
    }

    @Test
    @DisplayName("진료과 누락 시 LlmServiceUnavailableException 발생")
    void analyzeSymptom_진료과누락_예외발생() {
        // given - 진료과 라인 없음
        String incompleteLlmText = "전문의: 김내과의사\n시간: 09:30";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(incompleteLlmText)));

        // when & then
        assertThatThrownBy(() -> symptomAnalysisService.analyzeSymptom("두통").block())
                .isInstanceOf(LlmServiceUnavailableException.class)
                .hasMessageContaining("파싱 실패");
    }

    @Test
    @DisplayName("전문의 누락 시 LlmServiceUnavailableException 발생")
    void analyzeSymptom_전문의누락_예외발생() {
        // given - 전문의 라인 없음
        String incompleteLlmText = "진료과: 외과\n시간: 10:00";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(incompleteLlmText)));

        // when & then
        assertThatThrownBy(() -> symptomAnalysisService.analyzeSymptom("배가 아파요").block())
                .isInstanceOf(LlmServiceUnavailableException.class);
    }

    @Test
    @DisplayName("시간 누락 시 기본값 09:00 반환")
    void analyzeSymptom_시간누락_기본값사용() {
        // given - 시간 라인 없음
        String llmText = "진료과: 소아과\n전문의: 박소아";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("아이가 열이 나요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTime()).isEqualTo("09:00");
        assertThat(response.getDept()).isEqualTo("소아과");
    }

    @Test
    @DisplayName("전문의 이름에 대괄호 포함 시 제거 후 반환")
    void analyzeSymptom_전문의대괄호_제거() {
        // given
        String llmText = "진료과: 이비인후과\n전문의: [이비인후과전문의]\n시간: 11:00";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("목이 아파요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDoctor()).doesNotContain("[").doesNotContain("]");
        assertThat(response.getDoctor()).isEqualTo("이비인후과전문의");
    }
}
