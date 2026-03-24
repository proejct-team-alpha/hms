package com.smartclinic.hms.llm.service;

import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * 병원규칙 인덱싱 서비스 — Admin 규칙 CUD 시 Python LLM 서버에 인덱싱 요청
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RuleIndexService {

    private final WebClient llmWebClient;

    private static final Map<HospitalRuleCategory, String> CATEGORY_LABEL = Map.of(
            HospitalRuleCategory.EMERGENCY, "응급",
            HospitalRuleCategory.SUPPLY, "물품/비품",
            HospitalRuleCategory.DUTY, "당직/근무",
            HospitalRuleCategory.HYGIENE, "위생/감염",
            HospitalRuleCategory.OTHER, "기타"
    );

    /**
     * 규칙 생성/수정 후 인덱싱 요청 (비동기 — 실패해도 규칙 저장에 영향 없음)
     */
    public void indexRule(HospitalRule rule) {
        String category = CATEGORY_LABEL.getOrDefault(rule.getCategory(), "기타");

        llmWebClient.post()
                .uri("/index/rule")
                .bodyValue(Map.of(
                        "rule_id", rule.getId(),
                        "title", rule.getTitle(),
                        "content", rule.getContent(),
                        "category", category,
                        "target", rule.getTarget() != null ? rule.getTarget() : "",
                        "active", rule.isActive()
                ))
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        res -> log.info("규칙 인덱싱 완료 - ruleId: {}", rule.getId()),
                        err -> log.error("규칙 인덱싱 실패 - ruleId: {}, error: {}", rule.getId(), err.getMessage())
                );
    }

    /**
     * 규칙 삭제 후 인덱스 삭제 요청 (비동기)
     */
    public void deleteRuleIndex(Long ruleId) {
        llmWebClient.delete()
                .uri("/index/rule/{ruleId}", ruleId)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                        res -> log.info("규칙 인덱스 삭제 완료 - ruleId: {}", ruleId),
                        err -> log.error("규칙 인덱스 삭제 실패 - ruleId: {}, error: {}", ruleId, err.getMessage())
                );
    }
}
