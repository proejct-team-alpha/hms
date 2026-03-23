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

    @PostMapping("/use-batch")
    @ResponseBody
    public ResponseEntity<?> useItemBatch(Authentication auth,
                                          @RequestBody Map<String, Object> body) {
        try {
            // 1. 요청 데이터 추출
            List<Map<String, Object>> requests = (List<Map<String, Object>>) body.get("requests");
            
            // 2. 서비스 호출 (일괄 처리 수행)
            // reservationId는 스태프 출고이므로 null을 전달함
            Map<String, Object> serviceResult = itemManagerService.useItemsBatch(requests, null);
            
            // 3. [수정] 불변 맵 오류(NPE 및 Immutable 예외) 방지를 위해 가변 HashMap 새롭게 생성
            Map<String, Object> finalResponse = new java.util.HashMap<>();
            
            // 4. 처리 결과(성공/실패 목록) 담기
            finalResponse.put("successes", serviceResult.get("successes"));
            finalResponse.put("errors", serviceResult.get("errors"));
            
            // 5. [중요] 스태프 도메인에 맞게 오늘 해당 사용자의 전체 출고 내역 조회하여 반환
            // 이를 통해 오늘 출고 리스트와 '사용자' 정보가 실시간으로 갱신됨
            finalResponse.put("logs", itemManagerService.getTodayUsageLogsByUser(auth.getName()));
            
            // 6. 최신 재고 상태를 포함한 전체 물품 리스트 반환
            finalResponse.put("items", itemManagerService.getItemList(null));
            
            return ResponseEntity.ok(finalResponse);
        } catch (Exception e) {
            // 예외 발생 시 에러 메시지 반환
            return ResponseEntity.badRequest().body(Map.of("error", "일괄 처리 중 오류가 발생했습니다: " + e.getMessage()));
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
