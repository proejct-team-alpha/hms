package com.smartclinic.hms.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * Claude API 설정 — API Key 주입 및 RestClient 빈 구성
 * ════════════════════════════════════════════════════════════════════════════
 *
 * ■ 환경 변수 주입 (rule_spring.md §1)
 * CLAUDE_API_KEY : Anthropic API Key (필수, application.properties에서 참조)
 *
 * ■ 사용처
 * llm/symptom/ — 비회원 증상 분석 (§4, POST /llm/symptom/analyze)
 * llm/rules/ — 병원 규칙 챗봇 (§8, POST /llm/rules/ask)
 *
 * ■ 타임아웃
 * API 명세서 §4, §8: 5초 초과 시 폴백 응답 반환.
 * LLM 서비스 레이어에서 RestClient 호출 시 자동 적용.
 *
 * ■ LLM 서비스 사용 방법
 * private final RestClient claudeRestClient;
 * @Value("${claude.api.model}") private String model;
 * ════════════════════════════════════════════════════════════════════════════
 */
@Configuration
public class ClaudeApiConfig {

    private static final String ANTHROPIC_BASE_URL = "https://api.anthropic.com";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /** 환경 변수 CLAUDE_API_KEY 로 주입 */
    @Value("${claude.api.key}")
    private String apiKey;

    /** 연결·읽기 타임아웃 (기본 5초, application.properties 변경 가능) */
    @Value("${claude.api.timeout-seconds:5}")
    private int timeoutSeconds;

    /**
     * Claude API 전용 RestClient 빈.
     *
     * <ul>
     * <li>Base URL : {@code https://api.anthropic.com}</li>
     * <li>인증 헤더 : {@code x-api-key} (환경변수 CLAUDE_API_KEY)</li>
     * <li>API 버전 : {@code anthropic-version: 2023-06-01}</li>
     * <li>타임아웃 : {@code claude.api.timeout-seconds} (기본 5초)</li>
     * </ul>
     *
     * <p>
     * LLM 서비스 사용 예:
     * </p>
     * 
     * <pre>
     * &#64;RequiredArgsConstructor
     * &#64;Service
     * public class SymptomAnalysisService {
     *     private final RestClient claudeRestClient;
     *     &#64;Value("${claude.api.model}") private String model;
     *
     *     public AnalysisResult analyze(String symptomText) {
     *         return claudeRestClient.post()
     *             .uri("/v1/messages")
     *             .body(Map.of("model", model, "messages", ...))
     *             .retrieve()
     *             .body(ClaudeResponse.class);
     *     }
     * }
     * </pre>
     */
    @Bean
    RestClient claudeRestClient() {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(ANTHROPIC_BASE_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .requestFactory(requestFactory)
                .build();
    }
}
