package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleListResponse;
import com.smartclinic.hms.admin.rule.dto.CreateAdminRuleRequest;
import com.smartclinic.hms.common.util.SsrValidationViewSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/rule")
public class AdminRuleController {

    private static final String RULE_LIST_TITLE = "병원 규칙 관리";
    private static final String RULE_FORM_TITLE = "규칙 등록";

    private static final String INVALID_CATEGORY_MESSAGE = "\uC62C\uBC14\uB978 \uCE74\uD14C\uACE0\uB9AC\uB97C \uC120\uD0DD\uD574 \uC8FC\uC138\uC694.";

    private final AdminRuleService adminRuleService;

    @GetMapping("/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "category", defaultValue = "ALL") String category,
            @RequestParam(name = "active", defaultValue = "ALL") String active,
            @RequestParam(name = "keyword", required = false) String keyword,
            HttpServletRequest req) {
        AdminRuleListResponse result = adminRuleService.getRuleList(page, size, category, active, keyword);
        req.setAttribute("model", result);
        req.setAttribute("pageTitle", RULE_LIST_TITLE);
        return "admin/rule-list";
    }

    @GetMapping("/new")
    public String newForm(HttpServletRequest req, Model model) {
        return renderForm(req, model, CreateAdminRuleRequest.defaultForm());
    }

    @GetMapping("/form")
    public RedirectView legacyForm() {
        return redirectTo("/admin/rule/new");
    }

    @PostMapping("/new")
    public Object createNew(
            @Valid @ModelAttribute("model") CreateAdminRuleRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest req) {
        return handleCreate(request, bindingResult, redirectAttributes, model, req);
    }

    @PostMapping("/form")
    public Object createLegacyForm(
            @Valid @ModelAttribute("model") CreateAdminRuleRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest req) {
        return handleCreate(request, bindingResult, redirectAttributes, model, req);
    }

    private Object handleCreate(
            CreateAdminRuleRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest req) {
        if (bindingResult.hasErrors()) {
            applyFormErrors(req, bindingResult);
            return renderForm(req, model, request);
        }

        String successMessage = adminRuleService.createRule(request);
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return redirectTo("/admin/rule/list");
    }

    private String renderForm(HttpServletRequest req, Model model, CreateAdminRuleRequest request) {
        req.setAttribute("model", request);
        req.setAttribute("activeChecked", request.isActiveChecked());
        model.addAttribute("pageTitle", RULE_FORM_TITLE);
        model.addAttribute("model", request);
        model.addAttribute("titleValue", safeText(request.title()));
        model.addAttribute("contentValue", safeText(request.content()));
        model.addAttribute("activeChecked", request.isActiveChecked());
        model.addAttribute("emergencySelected", isCategorySelected(request, "EMERGENCY"));
        model.addAttribute("supplySelected", isCategorySelected(request, "SUPPLY"));
        model.addAttribute("dutySelected", isCategorySelected(request, "DUTY"));
        model.addAttribute("hygieneSelected", isCategorySelected(request, "HYGIENE"));
        model.addAttribute("otherSelected", isCategorySelected(request, "OTHER"));
        return "admin/rule-new";
    }

    private void applyFormErrors(HttpServletRequest req, BindingResult bindingResult) {
        SsrValidationViewSupport.applyErrors(req, bindingResult);

        FieldError categoryError = bindingResult.getFieldError("category");
        if (categoryError != null && isTypeMismatchError(categoryError)) {
            req.setAttribute("categoryError", INVALID_CATEGORY_MESSAGE);
        }
    }

    private boolean isTypeMismatchError(FieldError fieldError) {
        if (fieldError.getCode() != null && fieldError.getCode().startsWith("typeMismatch")) {
            return true;
        }

        String[] codes = fieldError.getCodes();
        if (codes == null) {
            return false;
        }

        for (String code : codes) {
            if (code != null && code.startsWith("typeMismatch")) {
                return true;
            }
        }
        return false;
    }

    private boolean isCategorySelected(CreateAdminRuleRequest request, String categoryName) {
        return Objects.nonNull(request.category()) && request.category().name().equals(categoryName);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private RedirectView redirectTo(String url) {
        RedirectView redirectView = new RedirectView(url);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
