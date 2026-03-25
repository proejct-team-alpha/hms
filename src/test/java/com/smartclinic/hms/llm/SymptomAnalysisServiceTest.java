package com.smartclinic.hms.llm;

import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.llm.dto.LlmResponse;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import com.smartclinic.hms.llm.service.SymptomAnalysisService;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("증상 분석 서비스 단위 테스트")
class SymptomAnalysisServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    WebClient llmWebClient;

    @Mock
    DepartmentRepository departmentRepository;

    @InjectMocks
    SymptomAnalysisService symptomAnalysisService;

    private Department makeDept(Long id, String name) {
        return Department.create(name, true);
    }

    @Test
    @DisplayName("정상 LLM 응답 - 진료과명 및 이유 추출, DB 매칭 성공")
    void analyzeSymptom_정상응답_매칭성공() {
        // given
        String llmText = "진료과: 내과\n이유: 소화기 관련 증상으로 내과 진료가 적합합니다.";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        Department dept = Department.create("내과", true);
        given(departmentRepository.findAll()).willReturn(List.of(dept));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("두통이 있어요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDepartmentName()).isEqualTo("내과");
        assertThat(response.getReason()).isEqualTo("소화기 관련 증상으로 내과 진료가 적합합니다.");
    }

    @Test
    @DisplayName("진료과 패턴 누락 시 텍스트 내 진료과명 직접 탐색")
    void analyzeSymptom_패턴누락_텍스트탐색() {
        // given - "진료과:" 패턴 없이 본문에 진료과명 포함
        String llmText = "증상을 보면 외과 진료가 필요합니다.";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        Department dept = Department.create("외과", true);
        given(departmentRepository.findAll()).willReturn(List.of(dept));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("팔이 부러졌어요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDepartmentName()).isEqualTo("외과");
    }

    @Test
    @DisplayName("매칭 실패 시 기본값 내과 사용")
    void analyzeSymptom_매칭실패_기본값() {
        // given - 알려진 진료과명이 없는 응답
        String llmText = "증상이 복합적이므로 정밀 검사가 필요합니다.";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        Department dept = Department.create("내과", true);
        given(departmentRepository.findAll()).willReturn(List.of(dept));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("잘 모르겠어요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDepartmentName()).isEqualTo("내과");
    }

    @Test
    @DisplayName("소아과 매칭 - 대괄호 포함 응답 처리")
    void analyzeSymptom_대괄호포함_정상처리() {
        // given
        String llmText = "진료과: [소아과]";
        given(llmWebClient.post().uri(anyString()).bodyValue(any()).retrieve()
                .bodyToMono(LlmResponse.class))
                .willReturn(Mono.just(new LlmResponse(llmText)));

        Department dept = Department.create("소아과", true);
        given(departmentRepository.findAll()).willReturn(List.of(dept));

        // when
        SymptomResponse response = symptomAnalysisService.analyzeSymptom("아이가 열이 나요").block();

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDepartmentName()).isEqualTo("소아과");
    }
}
