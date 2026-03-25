package com.smartclinic.hms.llm.service;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.common.exception.LlmServiceUnavailableException;
import com.smartclinic.hms.common.exception.LlmTimeoutException;
import com.smartclinic.hms.domain.ChatbotHistory;
import com.smartclinic.hms.domain.ChatbotHistoryRepository;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.llm.dto.ChatbotHistoryResponse;
import com.smartclinic.hms.llm.dto.LlmResponse;
import io.netty.channel.ConnectTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
@Slf4j
public class ChatService {

    private final WebClient llmWebClient;
    private final ChatbotHistoryRepository chatbotHistoryRepository;
    private final StaffRepository staffRepository;

    public Mono<String> callRuleLlmApi(String query, java.util.List<Map<String, String>> history) {
        log.debug("Rule LLM API 호출 시작 - query: {}, historySize: {}", query, history != null ? history.size() : 0);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("query", query);
        body.put("max_length", 1024);
        body.put("temperature", 0.3);
        if (history != null && !history.isEmpty()) {
            body.put("history", history.subList(Math.max(0, history.size() - 6), history.size()));
        }

        return llmWebClient.post()
                .uri("/infer/rule")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .map(LlmResponse::getGeneratedText)
                .onErrorMap(WebClientRequestException.class, e -> {
                    if (e.getCause() instanceof ConnectTimeoutException) {
                        return new LlmTimeoutException("Rule LLM 서버 연결 타임아웃", e);
                    }
                    return new LlmServiceUnavailableException("Rule LLM 서버 연결 실패", e);
                })
                .onErrorMap(TimeoutException.class, e ->
                        new LlmTimeoutException("Rule LLM 응답 시간 초과", e));
    }

    public Flux<String> callRuleLlmApiStream(String query, java.util.List<Map<String, String>> history) {
        log.debug("Rule LLM Stream API 호출 시작 - query: {}, historySize: {}", query, history != null ? history.size() : 0);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("query", query);
        body.put("max_length", 1024);
        body.put("temperature", 0.3);
        if (history != null && !history.isEmpty()) {
            body.put("history", history.subList(Math.max(0, history.size() - 6), history.size()));
        }

        return llmWebClient.post()
                .uri("/infer/rule/stream")
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .onErrorMap(WebClientRequestException.class, e -> {
                    if (e.getCause() instanceof ConnectTimeoutException) {
                        return new LlmTimeoutException("Rule LLM 서버 연결 타임아웃", e);
                    }
                    return new LlmServiceUnavailableException("Rule LLM 서버 연결 실패", e);
                })
                .onErrorMap(TimeoutException.class, e ->
                        new LlmTimeoutException("Rule LLM 응답 시간 초과", e));
    }

    @Transactional
    public ChatbotHistory saveChatHistory(Long staffId, String sessionId, String question, String answer) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));

        ChatbotHistory history = ChatbotHistory.create(sessionId, staff, question, answer);
        ChatbotHistory saved = chatbotHistoryRepository.save(history);
        log.debug("ChatbotHistory 저장 - id: {}, staffId: {}", saved.getId(), staffId);
        return saved;
    }

    /**
     * 규칙 챗봇 대화 히스토리 페이징 조회 (Controller는 Repository를 직접 호출하지 않는다).
     */
    public Page<ChatbotHistoryResponse> getRuleHistory(Long staffId, Pageable pageable) {
        return chatbotHistoryRepository.findByStaff_IdOrderByCreatedAtDesc(staffId, pageable)
                .map(ChatbotHistoryResponse::from);
    }
}
