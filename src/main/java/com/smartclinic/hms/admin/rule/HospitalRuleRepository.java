package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.domain.HospitalRule;
import com.smartclinic.hms.domain.HospitalRuleCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HospitalRuleRepository extends JpaRepository<HospitalRule, Long> {

    List<HospitalRule> findAllByOrderByCreatedAtDesc();

    @Query("""
            select r
            from HospitalRule r
            where (:category is null or r.category = :category)
              and (:active is null or r.active = :active)
              and (:keyword = '' or lower(r.title) like lower(concat('%', :keyword, '%')))
            order by r.createdAt desc, r.id desc
            """)
    Page<HospitalRule> search(
            @Param("category") HospitalRuleCategory category,
            @Param("active") Boolean active,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}