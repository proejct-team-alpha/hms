package com.smartclinic.hms.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotHistoryRepository extends JpaRepository<ChatbotHistory, Long> {
    List<ChatbotHistory> findByStaffIdOrderByCreatedAtDesc(Long staffId);
    List<ChatbotHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
