package com.smartclinic.hms.llm.controller;

import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.SecurityUtils;
import com.smartclinic.hms.domain.MedicalHistory;
import com.smartclinic.hms.llm.dto.*;
import com.smartclinic.hms.llm.service.DoctorService;
import com.smartclinic.hms.llm.service.LlmResponseParser;
import com.smartclinic.hms.llm.service.MedicalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/medical")
@Slf4j
public class MedicalController {

    private final MedicalService medicalService;
    private final DoctorService doctorService;
    private final LlmResponseParser llmResponseParser;
    private final SecurityUtils securityUtils;

    @PostMapping("/query")
    public Mono<String> handleMedicalQuery(@RequestBody LlmRequest request) {
        Long staffId = securityUtils.resolveStaffId();
        log.debug("Medical LLM 쿼리 수신 - query: {}, staffId: {}", request.getQuery(), staffId);

        MedicalHistory history = medicalService.saveMedicalPending(request.getQuery(), staffId);
        long startTime = System.currentTimeMillis();

        return medicalService.callMedicalLlmApi(request.getQuery())
                .doOnNext(response -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    medicalService.updateMedicalCompleted(history.getId(), response, latencyMs);
                    log.info("Medical LLM 응답 완료 - historyId: {}, latency: {}ms", history.getId(), latencyMs);
                })
                .doOnError(error -> {
                    medicalService.updateMedicalFailed(history.getId(), error.getMessage());
                    log.error("Medical LLM 호출 실패 - historyId: {}, error: {}", history.getId(), error.getMessage());
                });
    }

    @PostMapping("/query/consult")
    public Mono<MedicalLlmResponse> handleMedicalQueryWithDoctors(@RequestBody LlmRequest request) {
        Long staffId = securityUtils.resolveStaffId();
        log.debug("Medical+Doctor 쿼리 수신 - query: {}, staffId: {}", request.getQuery(), staffId);

        MedicalHistory history = medicalService.saveMedicalPending(request.getQuery(), staffId);
        long startTime = System.currentTimeMillis();

        return medicalService.callMedicalLlmApi(request.getQuery())
                .map(response -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    medicalService.updateMedicalCompleted(history.getId(), response, latencyMs);

                    String department = llmResponseParser.extractDepartment(response);
                    String reason = llmResponseParser.extractRecommendationReason(response);
                    List<DoctorWithScheduleDto> doctors = department != null
                            ? doctorService.findDoctorsWithSchedule(department) : List.of();

                    log.info("Medical+Doctor 응답 - dept: {}, doctors: {}, latency: {}ms",
                            department, doctors.size(), latencyMs);
                    return new MedicalLlmResponse(response, department, reason, doctors);
                })
                .doOnError(error -> {
                    medicalService.updateMedicalFailed(history.getId(), error.getMessage());
                    log.error("Medical+Doctor 호출 실패 - historyId: {}, error: {}", history.getId(), error.getMessage());
                });
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> handleMedicalQueryStream(@RequestBody LlmRequest request) {
        log.debug("Medical Stream 쿼리 수신 - query: {}", request.getQuery());
        return medicalService.callMedicalLlmApiStream(request.getQuery());
    }

    @GetMapping("/history/{staffId}")
    public Page<MedicalHistoryResponse> getMedicalHistory(
            @PathVariable Long staffId,
            @PageableDefault(size = 20) Pageable pageable) {

        Long authenticatedStaffId = securityUtils.resolveStaffId();
        if (authenticatedStaffId == null || !authenticatedStaffId.equals(staffId)) {
            throw CustomException.forbidden("본인의 히스토리만 조회할 수 있습니다.");
        }

        log.debug("의학 히스토리 조회 - staffId: {}, page: {}", staffId, pageable.getPageNumber());
        return medicalService.getMedicalHistory(staffId, pageable);
    }

}
