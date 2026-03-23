package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleDeleteResponse;
import com.smartclinic.hms.common.util.Resp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/api/rules")
public class AdminRuleApiController {

    private final AdminRuleService adminRuleService;

    @PostMapping("/{id}")
    public ResponseEntity<Resp<AdminRuleDeleteResponse>> deleteRule(
            @PathVariable("id") Long ruleId) {
        AdminRuleDeleteResponse response = adminRuleService.deleteRule(ruleId);
        return Resp.ok(response);
    }
}
