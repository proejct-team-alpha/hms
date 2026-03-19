package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_qa")
@Getter
@Setter
@NoArgsConstructor
public class MedicalQa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qa_id", nullable = false)
    private Integer qaId;

    @Column(nullable = false)
    private Integer domain;

    @Column(nullable = false, length = 50)
    private String department;

    @Column(name = "q_type", nullable = false)
    private Integer qType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(nullable = false, length = 20)
    private String dataset;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
