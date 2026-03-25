package com.smartclinic.hms.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_content")
@Getter
@NoArgsConstructor
public class MedicalContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "c_id", nullable = false, length = 50)
    private String cId;

    @Column(nullable = false)
    private Integer domain;

    private Integer source;

    @Column(name = "source_spec", length = 255)
    private String sourceSpec;

    @Column(name = "creation_year", length = 10)
    private String creationYear;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(nullable = false, length = 20)
    private String dataset;

    @Column(name = "data_type", nullable = false, length = 20)
    private String dataType;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
