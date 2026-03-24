package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/item")
public class AdminItemController {

    private final AdminItemService adminItemService;
    private final ItemManagerService itemManagerService;

    @GetMapping("/list")
    public String list(@RequestParam(name = "category", required = false) String category,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        List<?> items = adminItemService.getItemList(category, keyword);
        model.addAttribute("items", items);
        model.addAttribute("totalCount", items.size());
        model.addAttribute("categoryFilters", adminItemService.getCategoryFilters(category));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("pageTitle", "물품 목록");
        model.addAttribute("isAdminItemList", true);
        return "admin/item-list";
    }

    @GetMapping("/form")
    public String form(@RequestParam(name = "id", required = false) Long id, Model model) {
        model.addAttribute("form", adminItemService.getItemForm(id));
        model.addAttribute("pageTitle", id == null ? "물품 등록" : "물품 수정");
        model.addAttribute("isAdminItemList", true);
        model.addAttribute("isAdminItemForm", true);
        return "admin/item-form";
    }

    @PostMapping("/form/save")
    public String save(@RequestParam(name = "id", required = false) Long id,
                       @RequestParam("name") String name,
                       @RequestParam("category") String category,
                       @RequestParam("quantity") String quantityStr,
                       @RequestParam("minQuantity") String minQuantityStr,
                       RedirectAttributes ra) {
        String redirectForm = "redirect:/admin/item/form" + (id != null ? "?id=" + id : "");
        try {
            int quantity = parseQuantity(quantityStr, "재고 수량");
            int minQuantity = parseQuantity(minQuantityStr, "최소 수량");
            adminItemService.saveItem(id, name, category, quantity, minQuantity);
            ra.addFlashAttribute("message", "물품을 등록했습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return redirectForm;
        }
        return "redirect:/admin/item/list";
    }

    @PostMapping("/restock")
    public String restock(@RequestParam("id") Long id,
                          @RequestParam("amount") String amountStr,
                          RedirectAttributes ra) {
        try {
            int amount = parseQuantity(amountStr, "입고 수량");
            adminItemService.restockItem(id, amount);
            ra.addFlashAttribute("message", "물품을 입고했습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/item/list";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id, RedirectAttributes ra) {
        adminItemService.deleteItem(id);
        ra.addFlashAttribute("message", "물품을 삭제했습니다.");
        return "redirect:/admin/item/list";
    }

    @GetMapping("/use")
    public String itemUsePage(Model model) {
        List<ItemUsageLogDto> usageLogs = itemManagerService.getTodayStaffUsageLogs();
        long totalUsedAmount = itemManagerService.getTodayTotalStaffUsageAmount();

        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("todayLogs", usageLogs);
        model.addAttribute("todayLogCount", usageLogs.size());
        model.addAttribute("todayTotalUsedAmount", totalUsedAmount);
        model.addAttribute("pageTitle", "물품 출고");
        model.addAttribute("isAdminItemList", true);
        model.addAttribute("isAdminItemUse", true);
        return "admin/item-use";
    }

    @PostMapping("/use")
    @ResponseBody
    public ResponseEntity<?> useItem(@RequestParam("id") Long id,
                                     @RequestParam("amount") String amountStr) {
        try {
            long parsed = Long.parseLong(amountStr.trim());
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해 주세요."));
            }

            int newQuantity = itemManagerService.useItem(id, (int) parsed, null);
            List<ItemUsageLogDto> logs = itemManagerService.getTodayStaffUsageLogs();
            long totalUsedAmount = itemManagerService.getTodayTotalStaffUsageAmount();
            return ResponseEntity.ok(Map.of(
                    "quantity", newQuantity,
                    "logs", logs,
                    "todayLogCount", logs.size(),
                    "todayTotalUsedAmount", totalUsedAmount));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해 주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public String history(@RequestParam(name = "fromDate", required = false) String fromDate,
                          @RequestParam(name = "toDate", required = false) String toDate,
                          Model model) {
        DateRange dateRange = resolveDateRange(fromDate, toDate);
        List<?> histories = List.of();
        long totalIn = 0L;
        long totalOut = 0L;

        if (!dateRange.hasError()) {
            histories = itemManagerService.getStockHistory(dateRange.parsedFromDate(), dateRange.parsedToDate());
            totalIn = itemManagerService.getTotalInAmount(dateRange.parsedFromDate(), dateRange.parsedToDate());
            totalOut = itemManagerService.getTotalOutAmount(dateRange.parsedFromDate(), dateRange.parsedToDate());
        }

        model.addAttribute("fromDate", dateRange.fromDate());
        model.addAttribute("toDate", dateRange.toDate());
        if (dateRange.hasError()) {
            model.addAttribute("dateError", dateRange.errorMessage());
        }
        model.addAttribute("histories", histories);
        model.addAttribute("hasHistories", !histories.isEmpty());
        model.addAttribute("totalCount", histories.size());
        model.addAttribute("totalIn", totalIn);
        model.addAttribute("totalOut", totalOut);
        model.addAttribute("pageTitle", "입출고 내역");
        model.addAttribute("isAdminItemHistory", true);
        return "admin/item-history";
    }

    private DateRange resolveDateRange(String fromDate, String toDate) {
        LocalDate today = LocalDate.now();
        boolean fromBlank = isBlank(fromDate);
        boolean toBlank = isBlank(toDate);
        boolean useTodayDefault = fromBlank && toBlank;
        String resolvedFromDate = useTodayDefault ? today.toString() : normalizeDateValue(fromDate);
        String resolvedToDate = useTodayDefault ? today.toString() : normalizeDateValue(toDate);

        if (fromBlank != toBlank) {
            return DateRange.error(resolvedFromDate, resolvedToDate, "시작일과 종료일을 모두 입력해 주세요.");
        }

        try {
            LocalDate parsedFromDate = LocalDate.parse(resolvedFromDate);
            LocalDate parsedToDate = LocalDate.parse(resolvedToDate);

            if (parsedFromDate.isAfter(parsedToDate)) {
                return DateRange.error(resolvedFromDate, resolvedToDate, "시작일이 종료일보다 늦을 수 없습니다.");
            }

            return DateRange.success(resolvedFromDate, resolvedToDate, parsedFromDate, parsedToDate);
        } catch (DateTimeParseException e) {
            return DateRange.error(resolvedFromDate, resolvedToDate, "날짜 형식이 올바르지 않습니다.");
        }
    }

    private int parseQuantity(String value, String fieldName) {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 0 || parsed > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(fieldName + "은(는) 0 이상 2,147,483,647 이하여야 합니다.");
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "에 올바른 숫자를 입력해 주세요.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeDateValue(String value) {
        return value == null ? "" : value.trim();
    }

    private record DateRange(String fromDate,
                             String toDate,
                             LocalDate parsedFromDate,
                             LocalDate parsedToDate,
                             String errorMessage) {

        private static DateRange success(String fromDate, String toDate, LocalDate parsedFromDate, LocalDate parsedToDate) {
            return new DateRange(fromDate, toDate, parsedFromDate, parsedToDate, null);
        }

        private static DateRange error(String fromDate, String toDate, String errorMessage) {
            return new DateRange(fromDate, toDate, null, null, errorMessage);
        }

        private boolean hasError() {
            return errorMessage != null;
        }
    }
}