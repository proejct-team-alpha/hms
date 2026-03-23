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
            
            // [기능 구현] 개별 출고 시에도 전체 물품 리스트(items)를 반환하여 실시간 재고 동기화 지원
            return ResponseEntity.ok(Map.of(
                "items", itemManagerService.getItemList(null), 
                "logs", logs
            ));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "올바른 수량을 입력해주세요."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelItem(Authentication auth,
                                        @RequestParam("logId") Long logId) {
        try {
            // 특정 사용 로그 ID를 기반으로 출고를 취소하고 재고를 복구함
            itemManagerService.cancelItemUsage(logId);
            
            // 취소 후 갱신된 사용자의 오늘 출고 내역 조회
            List<ItemUsageLogDto> logs = itemManagerService.getTodayUsageLogsByUser(auth.getName());
            
            // 취소 후 복구된 최신 재고 상태를 포함하여 반환
            return ResponseEntity.ok(Map.of(
                "logs", logs,
                "items", itemManagerService.getItemList(null)
            ));
        } catch (Exception e) {
            // 예외 발생 시 에러 메시지 반환
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
