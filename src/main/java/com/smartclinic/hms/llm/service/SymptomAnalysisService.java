package com.smartclinic.hms.llm.service;

import com.smartclinic.hms.domain.Department;
import com.smartclinic.hms.llm.dto.LlmResponse;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import com.smartclinic.hms.reservation.reservation.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SymptomAnalysisService {

    private final WebClient llmWebClient;
    private final DepartmentRepository departmentRepository;

    // "추천 진료과:", "진료과:", "추천진료과:" 등 다양한 패턴 매칭
    private static final Pattern DEPT_PATTERN = Pattern.compile("(?:추천\\s*)?진료과\\s*[:：]\\s*(.+)");

    public Mono<SymptomResponse> analyzeSymptom(String symptomText) {
        List<String> deptNames = getActiveDepartmentNames();
        String deptList = String.join("|", deptNames);

        String prompt = """
                다음 환자의 증상을 분석하고, 가장 적합한 진료과를 하나만 추천하세요.
                반드시 아래 형식으로만 답변하세요:
                진료과: [진료과명]

                증상: %s
                """.formatted(symptomText);

        return llmWebClient.post()
                .uri("/infer/medical")
                .bodyValue(Map.of("query", prompt, "max_length", 64, "temperature", 0.1))
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .map(response -> extractDeptName(response.getGeneratedText(), deptNames))
                .flatMap(deptName -> Mono.fromCallable(() -> resolveDepartment(deptName))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * DB에서 활성화된 진료과명 목록 조회
     */
    private List<String> getActiveDepartmentNames() {
        return departmentRepository.findAll().stream()
                .filter(Department::isActive)
                .map(Department::getName)
                .toList();
    }

    /**
     * LLM 응답 텍스트에서 진료과명 추출
     * 1차: 정규식 매칭 ("진료과: 내과")
     * 2차: DB 진료과명이 텍스트에 포함되어 있는지 확인
     * 실패 시: DB 첫 번째 진료과 반환
     */
    private String extractDeptName(String text, List<String> deptNames) {
        log.info("LLM 응답 원문: {}", text);

        // 1차: 정규식
        Matcher m = DEPT_PATTERN.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim().replaceAll("[\\[\\]\\s]", "");
            for (String known : deptNames) {
                if (raw.contains(known)) {
                    log.info("정규식 매칭 진료과: {}", known);
                    return known;
                }
            }
        }

        // 2차: 텍스트 내 진료과명 직접 탐색
        for (String known : deptNames) {
            if (text.contains(known)) {
                log.info("텍스트 탐색 매칭 진료과: {}", known);
                return known;
            }
        }

        // 기본값: DB 첫 번째 진료과
        String fallback = deptNames.isEmpty() ? "내과" : deptNames.get(0);
        log.warn("진료과 매칭 실패, 기본값({}) 사용. 원문: {}", fallback, text);
        return fallback;
    }

    /**
     * 진료과명으로 DB 조회하여 SymptomResponse 생성
     */
    private SymptomResponse resolveDepartment(String deptName) {
        return departmentRepository.findAll().stream()
                .filter(d -> d.getName().equals(deptName))
                .findFirst()
                .map(dept -> new SymptomResponse(dept.getId(), dept.getName()))
                .orElseGet(() -> {
                    log.warn("DB에 진료과 '{}' 없음, 첫 번째 진료과 반환", deptName);
                    Department first = departmentRepository.findAll().get(0);
                    return new SymptomResponse(first.getId(), first.getName());
                });
    }
}
