package com.smartclinic.hms.admin.item;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.smartclinic.hms.item.ItemManagerService;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/item")
public class AdminItemController {

    private final AdminItemService adminItemService;
    private final ItemManagerService itemManagerService;

    @GetMapping("/list")
    public String list(@RequestParam(name = "category", required = false) String category, Model model) {
        model.addAttribute("items", adminItemService.getItemList(category));
        model.addAttribute("categoryFilters", adminItemService.getCategoryFilters(category));
        model.addAttribute("pageTitle", "물품 목록");
        model.addAttribute("isAdminItemList", true);
        return "admin/item-list";
    }

    @GetMapping("/form")
    public String form(@RequestParam(name = "id", required = false) Long id, Model model) {
        model.addAttribute("form", adminItemService.getItemForm(id));
        model.addAttribute("pageTitle", id == null ? "물품 등록" : "물품 수정");
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
            ra.addFlashAttribute("message", "물품이 저장되었습니다.");
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
            ra.addFlashAttribute("message", "물품이 입고되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/item/list";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id, RedirectAttributes ra) {
        adminItemService.deleteItem(id);
        ra.addFlashAttribute("message", "물품이 삭제되었습니다.");
        return "redirect:/admin/item/list";
    }

    @GetMapping("/use")
    public String itemUsePage(Model model) {
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("todayLogs", itemManagerService.getTodayStaffUsageLogs());
        model.addAttribute("pageTitle", "물품 출고");
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
                return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
            }
            int newQuantity = itemManagerService.useItem(id, (int) parsed, null);
            return ResponseEntity.ok(Map.of("quantity", newQuantity));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("pageTitle", "입출고 내역");
        model.addAttribute("isAdminItemHistory", true);
        return "admin/item-history";
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
}
