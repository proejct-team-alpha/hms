package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 챗봇 대화 이력 엔티티 (ERD §2.10)
 */
@Entity
@Table(name = "chatbot_history",
    indexes = @Index(name = "idx_chatbot_session", columnList = "session_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static ChatbotHistory create(String sessionId, Staff staff, String question, String answer) {
        ChatbotHistory ch = new ChatbotHistory();
        ch.sessionId = sessionId;
        ch.staff = staff;
        ch.question = question;
        ch.answer = answer;
        return ch;
    }
}
