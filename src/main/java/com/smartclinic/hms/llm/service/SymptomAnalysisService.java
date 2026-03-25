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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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

    // "추천 이유:", "이유:" 등 패턴 매칭
    private static final Pattern REASON_PATTERN = Pattern.compile("(?:추천\\s*)?이유\\s*[:：]\\s*(.+)");

    // 진료과 목록 캐시 (5분 TTL — 진료과는 자주 변경되지 않음)
    private static final long CACHE_TTL_MS = 5 * 60 * 1000L;
    private final AtomicReference<List<Department>> cachedDepts = new AtomicReference<>();
    private final AtomicLong cacheTimestamp = new AtomicLong(0);

    private List<Department> getActiveDepartments() {
        long now = System.currentTimeMillis();
        List<Department> cached = cachedDepts.get();
        if (cached != null && now - cacheTimestamp.get() < CACHE_TTL_MS) {
            return cached;
        }
        List<Department> depts = departmentRepository.findAll().stream()
                .filter(Department::isActive)
                .toList();
        cachedDepts.set(depts);
        cacheTimestamp.set(now);
        log.debug("진료과 캐시 갱신: {}개", depts.size());
        return depts;
    }

    public Mono<SymptomResponse> analyzeSymptom(String symptomText) {
        List<Department> departments = getActiveDepartments();
        List<String> deptNames = departments.stream().map(Department::getName).toList();

        String deptOptions = String.join("|", deptNames);

        String prompt = """
                당신은 환자의 증상을 분석하여 적합한 진료과를 추천하는 의료 안내 AI입니다.
                다음 환자의 증상을 분석하고, 가장 적합한 진료과를 하나만 추천하세요.

                [선택 가능한 진료과]
                %s

                반드시 아래 형식으로 답변하세요:
                진료과: (위 진료과 중 하나)
                이유: (환자의 증상과 추천 진료과의 연관성을 2~3문장으로 설명)

                환자 증상: %s
                """.formatted(deptOptions, symptomText);

        return llmWebClient.post()
                .uri("/infer/medical")
                .bodyValue(Map.of("query", prompt, "max_length", 256, "temperature", 0.1))
                .retrieve()
                .bodyToMono(LlmResponse.class)
                .map(response -> {
                    String text = response.getGeneratedText();
                    String dept = extractDeptName(text, deptNames);
                    String reason = extractReason(text);
                    return new String[]{dept, reason};
                })
                .map(parsed -> resolveDepartment(departments, parsed[0], parsed[1]));
    }

    /**
     * LLM 응답 텍스트에서 진료과명 추출
     * 1차: 정규식 매칭 ("진료과: 내과")
     * 2차: 알려진 진료과명이 텍스트에 포함되어 있는지 확인
     * 실패 시: 기본값 "내과"
     */
    private String extractDeptName(String text, List<String> knownDepts) {
        log.info("LLM 응답 원문: {}", text);

        // 1차: 정규식
        Matcher m = DEPT_PATTERN.matcher(text);
        if (m.find()) {
            String raw = m.group(1).trim().replaceAll("[\\[\\]\\s]", "");
            for (String known : knownDepts) {
                if (raw.contains(known)) {
                    log.info("정규식 매칭 진료과: {}", known);
                    return known;
                }
            }
        }

        // 2차: 텍스트 내 진료과명 직접 탐색
        for (String known : knownDepts) {
            if (text.contains(known)) {
                log.info("텍스트 탐색 매칭 진료과: {}", known);
                return known;
            }
        }

        // 기본값
        log.warn("진료과 매칭 실패, 기본값(내과) 사용. 원문: {}", text);
        return "내과";
    }

    /**
     * LLM 응답 텍스트에서 추천 이유 추출
     */
    private String extractReason(String text) {
        Matcher m = REASON_PATTERN.matcher(text);
        if (m.find()) {
            String reason = m.group(1).trim();
            // "이유:" 이후 줄바꿈 전까지 또는 전체 텍스트
            int nextLine = reason.indexOf('\n');
            if (nextLine > 0) {
                reason = reason.substring(0, nextLine).trim();
            }
            log.info("추천 이유 추출: {}", reason);
            return reason;
        }
        log.warn("추천 이유 추출 실패");
        return null;
    }

    /**
     * 캐시된 진료과 목록에서 이름으로 매칭하여 SymptomResponse 생성
     */
    private SymptomResponse resolveDepartment(List<Department> departments, String deptName, String reason) {
        return departments.stream()
                .filter(d -> d.getName().equals(deptName))
                .findFirst()
                .map(dept -> new SymptomResponse(dept.getId(), dept.getName(), reason))
                .orElseGet(() -> {
                    log.warn("진료과 '{}' 없음, 첫 번째 진료과 반환", deptName);
                    Department first = departments.get(0);
                    return new SymptomResponse(first.getId(), first.getName(), reason);
                });
    }
}
