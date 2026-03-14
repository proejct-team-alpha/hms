package com.smartclinic.hms.admin.rule;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRuleService {

    private final HospitalRuleRepository hospitalRuleRepository;

    public List<AdminRuleDto> getRuleList() {
        return hospitalRuleRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(AdminRuleDto::new)
                .collect(Collectors.toList());
    }
}
