package com.smartclinic.hms.staff.item;

import com.smartclinic.hms.item.ItemManagerService;
import com.smartclinic.hms.item.log.ItemUsageLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/staff/item")
public class StaffItemController {

    private final ItemManagerService itemManagerService;

    @GetMapping("/use")
    public String itemUsePage(Authentication auth, Model model) {
        model.addAttribute("items", itemManagerService.getItemList(null));
        model.addAttribute("todayLogs", itemManagerService.getTodayUsageLogsByUser(auth.getName()));
        model.addAttribute("pageTitle", "물품 출고");
        return "staff/item-use";
    }

    @PostMapping("/use")
    @ResponseBody
    public ResponseEntity<?> useItem(Authentication auth,
                                     @RequestParam("id") Long id,
                                     @RequestParam("amount") String amountStr) {
        try {
            long parsed = Long.parseLong(amountStr.trim());
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
            }
            int newQuantity = itemManagerService.useItem(id, (int) parsed, null);
            List<ItemUsageLogDto> logs = itemManagerService.getTodayUsageLogsByUser(auth.getName());
            return ResponseEntity.ok(Map.of("quantity", newQuantity, "logs", logs));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/restock")
    @ResponseBody
    public ResponseEntity<?> restockItem(@RequestParam("id") Long id,
                                         @RequestParam("amount") String amountStr) {
        try {
            long parsed = Long.parseLong(amountStr.trim());
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
            }
            int newQuantity = itemManagerService.restockItemAndGetQuantity(id, (int) parsed);
            return ResponseEntity.ok(Map.of("quantity", newQuantity));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
