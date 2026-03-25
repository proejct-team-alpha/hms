package com.smartclinic.hms.admin.rule;

import com.smartclinic.hms.admin.rule.dto.AdminRuleListResponse;
import com.smartclinic.hms.admin.rule.dto.CreateAdminRuleRequest;
import com.smartclinic.hms.admin.rule.dto.AdminRuleDetailResponse;
import com.smartclinic.hms.admin.rule.dto.UpdateAdminRuleRequest;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.common.util.SsrValidationViewSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private static final String RULE_DETAIL_TITLE = "규칙 상세";

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

    @GetMapping("/detail")
    public String detail(
            @RequestParam("ruleId") Long ruleId,
            HttpServletRequest req,
            HttpServletResponse response,
            Model model) {
        try {
            AdminRuleDetailResponse detail = adminRuleService.getRuleDetail(ruleId);
            return renderDetail(req, model, detail, UpdateAdminRuleRequest.from(detail));
        } catch (CustomException ex) {
            return renderNotFound(req, response, ex);
        }
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

    @PostMapping("/update")
    public Object update(
            @Valid @ModelAttribute("model") UpdateAdminRuleRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest req) {
        if (bindingResult.hasErrors()) {
            return renderUpdateValidationFailure(req, model, request, bindingResult, redirectAttributes);
        }

        try {
            String successMessage = adminRuleService.updateRule(request);
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            return redirectToDetail(request.ruleId());
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            if (ex.getHttpStatus() == HttpStatus.NOT_FOUND) {
                return redirectTo("/admin/rule/list");
            }
            return redirectToDetail(request.ruleId());
        }
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
        req.setAttribute("pageTitle", RULE_FORM_TITLE);
        req.setAttribute("activeChecked", request.isActiveChecked());
        model.addAttribute("pageTitle", RULE_FORM_TITLE);
        model.addAttribute("model", request);
        populateFormAttributes(
                model,
                null,
                request.title(),
                request.content(),
                request.category(),
                request.isActiveChecked(),
                "/admin/rule/new",
                "등록",
                "/admin/rule/list",
                false,
                "규칙 등록",
                "새 병원 규칙을 입력하세요."
        );
        return "admin/rule-new";
    }

    private String renderDetail(
            HttpServletRequest req,
            Model model,
            AdminRuleDetailResponse detail,
            UpdateAdminRuleRequest request) {
        req.setAttribute("rule", detail);
        req.setAttribute("model", request);
        req.setAttribute("pageTitle", RULE_DETAIL_TITLE);
        req.setAttribute("activeChecked", request.isActiveChecked());
        model.addAttribute("pageTitle", RULE_DETAIL_TITLE);
        model.addAttribute("rule", detail);
        model.addAttribute("model", request);
        populateFormAttributes(
                model,
                request.ruleId(),
                request.title(),
                request.content(),
                request.category(),
                request.isActiveChecked(),
                "/admin/rule/update",
                "저장",
                "/admin/rule/list",
                true,
                "규칙 수정",
                "상세 화면에서 규칙 내용을 바로 수정할 수 있습니다."
        );
        return "admin/rule-detail";
    }

    private Object renderUpdateValidationFailure(
            HttpServletRequest req,
            Model model,
            UpdateAdminRuleRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (request.ruleId() == null) {
            redirectAttributes.addFlashAttribute("errorMessage", SsrValidationViewSupport.INPUT_CHECK_MESSAGE);
            return redirectTo("/admin/rule/list");
        }

        try {
            AdminRuleDetailResponse detail = adminRuleService.getRuleDetail(request.ruleId());
            applyFormErrors(req, bindingResult);
            return renderDetail(req, model, detail, request);
        } catch (CustomException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return redirectTo("/admin/rule/list");
        }
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

    private void populateFormAttributes(
            Model model,
            Long ruleId,
            String title,
            String content,
            Enum<?> category,
            boolean activeChecked,
            String formAction,
            String submitLabel,
            String cancelUrl,
            boolean hasRuleId,
            String formSectionTitle,
            String formSectionDescription) {
        model.addAttribute("titleValue", safeText(title));
        model.addAttribute("contentValue", safeText(content));
        model.addAttribute("activeChecked", activeChecked);
        model.addAttribute("formAction", formAction);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("cancelUrl", cancelUrl);
        model.addAttribute("hasRuleId", hasRuleId);
        model.addAttribute("ruleIdValue", ruleId == null ? "" : ruleId);
        model.addAttribute("formSectionTitle", formSectionTitle);
        model.addAttribute("formSectionDescription", formSectionDescription);
        model.addAttribute("emergencySelected", isCategorySelected(category, "EMERGENCY"));
        model.addAttribute("supplySelected", isCategorySelected(category, "SUPPLY"));
        model.addAttribute("dutySelected", isCategorySelected(category, "DUTY"));
        model.addAttribute("hygieneSelected", isCategorySelected(category, "HYGIENE"));
        model.addAttribute("otherSelected", isCategorySelected(category, "OTHER"));
    }

    private boolean isCategorySelected(Enum<?> category, String categoryName) {
        return Objects.nonNull(category) && category.name().equals(categoryName);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String renderNotFound(HttpServletRequest req, HttpServletResponse response, CustomException ex) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        req.setAttribute("pageTitle", "페이지를 찾을 수 없습니다");
        req.setAttribute("errorMessage", ex.getMessage());
        req.setAttribute("path", req.getRequestURI());
        return "error/404";
    }

    private RedirectView redirectToDetail(Long ruleId) {
        return redirectTo("/admin/rule/detail?ruleId=" + ruleId);
    }

    private RedirectView redirectTo(String url) {
        RedirectView redirectView = new RedirectView(url);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
