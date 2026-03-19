package com.smartclinic.hms.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, Long> {
    Optional<MedicalHistory> findBySessionId(String sessionId);
    List<MedicalHistory> findByStaffIdOrderByCreatedAtDesc(Long staffId);
}
