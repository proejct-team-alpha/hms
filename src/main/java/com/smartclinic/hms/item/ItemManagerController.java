package com.smartclinic.hms.item;

import com.smartclinic.hms.item.dto.ItemDashboardDto;
import com.smartclinic.hms.item.dto.ItemListDto;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/item-manager")
public class ItemManagerController {

    private final ItemManagerService itemService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        ItemDashboardDto dashboard = itemService.getDashboard();
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("pageTitle", "물품 대시보드");
        return "item-manager/dashboard";
    }

    @GetMapping("/item-list")
    public String itemList(@RequestParam(name = "category", required = false) String category, Model model) {
        List<ItemListDto> items = itemService.getItemList(category);
        model.addAttribute("items", items);
        model.addAttribute("categoryFilters", itemService.getCategoryFilters(category));
        model.addAttribute("pageTitle", "물품 목록");
        return "item-manager/item-list";
    }

    @GetMapping("/item-form")
    public String itemForm(@RequestParam(name = "id", required = false) Long id, Model model) {
        model.addAttribute("form", itemService.getItemForm(id));
        model.addAttribute("pageTitle", id == null ? "물품 등록" : "물품 수정");
        return "item-manager/item-form";
    }

    @PostMapping("/item-form/save")
    public String saveItem(@RequestParam(name = "id", required = false) Long id,
                           @RequestParam("name") String name,
                           @RequestParam("category") String category,
                           @RequestParam("quantity") String quantityStr,
                           @RequestParam("minQuantity") String minQuantityStr,
                           RedirectAttributes ra) {
        String redirectForm = "redirect:/item-manager/item-form" + (id != null ? "?id=" + id : "");
        try {
            int quantity = parseQuantity(quantityStr, "재고 수량");
            int minQuantity = parseQuantity(minQuantityStr, "최소 수량");
            itemService.saveItem(id, name, category, quantity, minQuantity);
            ra.addFlashAttribute("message", "물품이 저장되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return redirectForm;
        }
        return "redirect:/item-manager/item-list";
    }

    private int parseQuantity(String value, String fieldName) {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 0 || parsed > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(fieldName + "은(는) 0 이상 2,147,483,647 이하여야 합니다.");
            }
            return (int) parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "에 올바른 숫자를 입력해주세요.");
        }
    }

    @PostMapping("/item/restock")
    public String restockItem(@RequestParam("id") Long id,
                              @RequestParam("amount") String amountStr,
                              @RequestParam(name = "redirectTo", defaultValue = "/item-manager/dashboard") String redirectTo,
                              RedirectAttributes ra) {
        try {
            int amount = parseQuantity(amountStr, "입고 수량");
            itemService.restockItem(id, amount);
            ra.addFlashAttribute("message", "물품이 입고되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:" + redirectTo;
    }

    @PostMapping("/item/restock/ajax")
    @ResponseBody
    public ResponseEntity<?> restockItemAjax(@RequestParam("id") Long id,
                                             @RequestParam("amount") String amountStr) {
        try {
            int amount = parseQuantity(amountStr, "입고 수량");
            int newQuantity = itemService.restockItemAndGetQuantity(id, amount);
            return ResponseEntity.ok(Map.of("quantity", newQuantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/item/delete")
    public String deleteItem(@RequestParam("id") Long id, RedirectAttributes ra) {
        itemService.deleteItem(id);
        ra.addFlashAttribute("message", "물품이 삭제되었습니다.");
        return "redirect:/item-manager/item-list";
    }

    @GetMapping("/item-use")
    public String itemUsePage(Model model) {
        model.addAttribute("items", itemService.getItemList(null));
        model.addAttribute("todayLogs", itemService.getTodayStaffUsageLogs());
        model.addAttribute("pageTitle", "물품 출고");
        return "item-manager/item-use";
    }

    @PostMapping("/item-use")
    @ResponseBody
    public ResponseEntity<?> useItem(@RequestParam("id") Long id,
                                     @RequestParam("amount") String amountStr) {
        try {
            long parsed = Long.parseLong(amountStr.trim());
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
            }
            int newQuantity = itemService.useItem(id, (int) parsed, null);
            List<ItemUsageLogDto> logs = itemService.getTodayStaffUsageLogs();
            return ResponseEntity.ok(Map.of("quantity", newQuantity, "logs", logs));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/item-history")
    public String itemHistory(@RequestParam(name = "fromDate", required = false) String fromDate,
                              @RequestParam(name = "toDate", required = false) String toDate,
                              Model model) {
        DateRange dateRange = resolveDateRange(fromDate, toDate);
        List<?> histories = List.of();
        long totalIn = 0L;
        long totalOut = 0L;

        if (!dateRange.hasError()) {
            histories = itemService.getStockHistory(dateRange.parsedFromDate(), dateRange.parsedToDate());
            totalIn = itemService.getTotalInAmount(dateRange.parsedFromDate(), dateRange.parsedToDate());
            totalOut = itemService.getTotalOutAmount(dateRange.parsedFromDate(), dateRange.parsedToDate());
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
        model.addAttribute("pageTitle", "물품 입출고 내역");
        model.addAttribute("isItemHistory", true);
        return "item-manager/item-history";
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
