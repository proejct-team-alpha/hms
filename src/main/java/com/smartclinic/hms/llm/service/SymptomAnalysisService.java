package com.smartclinic.hms.llm.service;

import com.smartclinic.hms.common.exception.LlmServiceUnavailableException;
import com.smartclinic.hms.llm.dto.LlmResponse;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymptomAnalysisService {

    private final WebClient llmWebClient;

    private static final Pattern DEPT_PATTERN   = Pattern.compile("진료과:\\s*(.+)");
    private static final Pattern DOCTOR_PATTERN = Pattern.compile("전문의:\\s*(.+)");
    private static final Pattern TIME_PATTERN   = Pattern.compile("시간:\\s*(\\d{2}:\\d{2})");

    public Mono<SymptomResponse> analyzeSymptom(String symptomText) {
        String prompt = """
                다음 증상을 분석하고 아래 형식으로만 답변하세요:
                진료과: [내과|외과|소아과|이비인후과 중 하나]
                전문의: [해당 진료과 의사 1명]
                시간: [09:00~15:30 사이 HH:mm]

                증상: %s
                """.formatted(symptomText);

        return llmWebClient.post()
                .uri("/infer/medical")
                .bodyValue(Map.of("query", prompt, "max_length", 64, "temperature", 0.1))
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .map(response -> parseText(response.getGeneratedText()));
    }

    private SymptomResponse parseText(String text) {
        String dept   = extract(DEPT_PATTERN, text);
        String doctor = extract(DOCTOR_PATTERN, text);
        String time   = extract(TIME_PATTERN, text);

        if (dept == null || doctor == null) {
            log.warn("LLM 응답 파싱 불완전. 원문: {}", text);
            throw new LlmServiceUnavailableException("증상 분석 결과 파싱 실패");
        }

        String cleanDoctor = doctor.trim().replaceAll("[\\[\\]]", "");
        String cleanTime   = (time != null) ? time.trim() : "09:00";

        return new SymptomResponse(dept.trim(), cleanDoctor, cleanTime);
    }

    private String extract(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
