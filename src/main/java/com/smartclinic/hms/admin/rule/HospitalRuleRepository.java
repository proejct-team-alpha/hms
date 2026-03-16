package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.domain.HospitalRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalRuleRepository extends JpaRepository<HospitalRule, Long> {

    List<HospitalRule> findAllByOrderByCreatedAtDesc();
}
