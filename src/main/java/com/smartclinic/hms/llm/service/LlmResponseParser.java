package com.smartclinic.hms.llm.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 응답 텍스트에서 추천 진료과명과 추천 이유를 추출하는 파서.
 */
@Component
@Slf4j
public class LlmResponseParser {

    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile(
            "추천\\s*진료과[*]*\\s*[:：]\\s*[*]*\\s*(.+?)\\s*[*]*\\s*$",
            Pattern.MULTILINE
    );

    private static final Pattern REASON_PATTERN = Pattern.compile(
            "추천\\s*진료과[*]*\\s*[:：].+?\\n+(.+?)(?=\\n\\s*\\n|\\n[-─*]{2,}|$)",
            Pattern.DOTALL
    );

    public String extractDepartment(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) return null;

        Matcher matcher = DEPARTMENT_PATTERN.matcher(llmResponse);
        if (matcher.find()) {
            String department = matcher.group(1).replaceAll("[*]", "").trim();
            log.info("Extracted department: {}", department);
            return department;
        }
        log.info("Could not extract department from LLM response");
        return null;
    }

    public String extractRecommendationReason(String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) return null;

        Matcher matcher = REASON_PATTERN.matcher(llmResponse);
        if (matcher.find()) return matcher.group(1).trim();
        return null;
    }
}
