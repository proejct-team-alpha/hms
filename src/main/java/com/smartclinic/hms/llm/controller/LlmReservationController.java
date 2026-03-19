package com.smartclinic.hms.llm.controller;

import com.smartclinic.hms.llm.dto.LlmReservationResponse;
import com.smartclinic.hms.llm.service.LlmReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/llm/reservation")
@Slf4j
public class LlmReservationController {

    private final LlmReservationService llmReservationService;

    @GetMapping("/slots/{doctorId}")
    public LlmReservationResponse.SlotList getAvailableSlots(@PathVariable Long doctorId) {
        log.debug("가용 슬롯 조회 - doctorId: {}", doctorId);
        return llmReservationService.getAvailableSlots(doctorId);
    }
}
