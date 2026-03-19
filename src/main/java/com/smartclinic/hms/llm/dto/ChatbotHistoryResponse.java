package com.smartclinic.hms.llm.dto;

import com.smartclinic.hms.domain.ChatbotHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatbotHistoryResponse {

    private Long id;
    private String sessionId;
    private String question;
    private String answer;
    private LocalDateTime createdAt;

    public static ChatbotHistoryResponse from(ChatbotHistory entity) {
        return new ChatbotHistoryResponse(
                entity.getId(),
                entity.getSessionId(),
                entity.getQuestion(),
                entity.getAnswer(),
                entity.getCreatedAt()
        );
    }
}
