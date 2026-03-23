package com.smartclinic.hms.llm.controller;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.SecurityUtils;
import com.smartclinic.hms.llm.dto.ChatbotHistoryResponse;
import com.smartclinic.hms.llm.dto.LlmRequest;
import com.smartclinic.hms.llm.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/chatbot")
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SecurityUtils securityUtils;

    @PostMapping("/query")
    public Mono<String> handleRuleQuery(
            @RequestBody LlmRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        Long staffId = securityUtils.resolveStaffId();
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

        Long authenticatedStaffId = securityUtils.resolveStaffId();
        if (authenticatedStaffId == null || !authenticatedStaffId.equals(staffId)) {
            throw CustomException.forbidden("본인의 히스토리만 조회할 수 있습니다.");
        }

        log.debug("챗봇 히스토리 조회 - staffId: {}, page: {}", staffId, pageable.getPageNumber());
        return chatService.getRuleHistory(staffId, pageable);
    }

}
