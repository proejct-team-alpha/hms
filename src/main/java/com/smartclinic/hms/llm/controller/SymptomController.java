package com.smartclinic.hms.llm.controller;

import com.smartclinic.hms.llm.dto.SymptomRequest;
import com.smartclinic.hms.llm.dto.SymptomResponse;
import com.smartclinic.hms.llm.service.SymptomAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/llm/symptom")
@RequiredArgsConstructor
public class SymptomController {

    private final SymptomAnalysisService symptomAnalysisService;

    @PostMapping("/analyze")
    public Mono<SymptomResponse> analyze(@RequestBody SymptomRequest request) {
        return symptomAnalysisService.analyzeSymptom(request.getSymptomText());
    }
}
