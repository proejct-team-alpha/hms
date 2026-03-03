package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 증상 추천 이력 엔티티 (ERD §2.9)
 */
@Entity
@Table(name = "llm_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symptom_text", nullable = false, columnDefinition = "TEXT")
    private String symptomText;

    @Column(name = "recommended_dept", length = 100)
    private String recommendedDept;

    @Column(name = "recommended_doctor", length = 100)
    private String recommendedDoctor;

    @Column(name = "recommended_time", length = 50)
    private String recommendedTime;

    @Column(name = "llm_response_raw", columnDefinition = "TEXT")
    private String llmResponseRaw;

    @Column(name = "is_used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static LlmRecommendation create(String symptomText, String recommendedDept,
                                          String recommendedDoctor, String recommendedTime,
                                          String llmResponseRaw) {
        LlmRecommendation r = new LlmRecommendation();
        r.symptomText = symptomText;
        r.recommendedDept = recommendedDept;
        r.recommendedDoctor = recommendedDoctor;
        r.recommendedTime = recommendedTime;
        r.llmResponseRaw = llmResponseRaw;
        return r;
    }

    public void markAsUsed() {
        this.used = true;
    }
}
