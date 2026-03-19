package com.smartclinic.hms.llm.controller;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.ChatbotHistoryRepository;
import com.smartclinic.hms.domain.Staff;
import com.smartclinic.hms.llm.dto.ChatbotHistoryResponse;
import com.smartclinic.hms.llm.dto.LlmRequest;
import com.smartclinic.hms.llm.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/chatbot")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ChatbotHistoryRepository chatbotHistoryRepository;
    private final StaffRepository staffRepository;

    @PostMapping("/query")
    public Mono<String> handleRuleQuery(
            @RequestBody LlmRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Long staffId = resolveStaffId();
        log.debug("Rule Q&A 쿼리 수신 - query: {}, staffId: {}", request.getQuery(), staffId);

        String effectiveSessionId = sessionId != null ? sessionId
                : "session-" + staffId + "-" + System.currentTimeMillis();

        return chatService.callRuleLlmApi(request.getQuery())
                .doOnNext(answer -> {
                    if (staffId != null) {
                        try {
                            chatService.saveChatHistory(staffId, effectiveSessionId, request.getQuery(), answer);
                            log.info("Rule Q&A 저장 완료 - staffId: {}", staffId);
                        } catch (Exception e) {
                            log.error("Rule Q&A 히스토리 저장 실패 - staffId: {}, error: {}", staffId, e.getMessage(), e);
                        }
                    }
                });
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleRuleQueryStream(@RequestBody LlmRequest request) {
        log.debug("Rule Stream 쿼리 수신 - query: {}", request.getQuery());
        return chatService.callRuleLlmApiStream(request.getQuery());
    }

    @GetMapping("/history/{staffId}")
    public Page<ChatbotHistoryResponse> getRuleHistory(
            @PathVariable Long staffId,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("챗봇 히스토리 조회 - staffId: {}, page: {}", staffId, pageable.getPageNumber());
        return chatbotHistoryRepository.findByStaff_IdOrderByCreatedAtDesc(staffId, pageable)
                .map(ChatbotHistoryResponse::from);
    }

    private Long resolveStaffId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return staffRepository.findByUsernameAndActiveTrue(auth.getName())
                .map(Staff::getId).orElse(null);
    }
}
