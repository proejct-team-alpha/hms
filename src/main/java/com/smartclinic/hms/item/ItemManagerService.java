package com.smartclinic.hms.item;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.item.dto.*;
import com.smartclinic.hms.item.log.*;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartclinic.hms.item.dto.ItemChartDayDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemManagerService {

    private final ItemManagerRepository itemRepository;
    private final ItemUsageLogRepository usageLogRepository;
    private final ItemStockLogRepository stockLogRepository;
    private final StaffRepository staffRepository;

    public ItemDashboardDto getDashboard() {
        List<Item> all = itemRepository.findAllByOrderByNameAsc();
        List<Item> lowStock = itemRepository.findLowStockItems();

        return new ItemDashboardDto(
                all.size(),
                lowStock.size(),
                lowStock.stream().limit(5).map(ItemListDto::new).toList(),
                buildChartDays()
        );
    }

    private List<ItemChartDayDto> buildChartDays() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<ItemStockLog> logs = stockLogRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(start, end);

        // 날짜별 IN/OUT 합산
        Map<LocalDate, int[]> map = new java.util.LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            map.put(today.minusDays(i), new int[]{0, 0}); // [IN, OUT]
        }
        for (ItemStockLog log : logs) {
            LocalDate date = log.getCreatedAt().toLocalDate();
            if (map.containsKey(date)) {
                if (log.getType() == ItemStockType.IN) map.get(date)[0] += log.getAmount();
                else map.get(date)[1] += log.getAmount();
            }
        }

        // 최대값 계산 (높이 정규화용)
        int max = map.values().stream()
                .mapToInt(v -> Math.max(v[0], v[1]))
                .max().orElse(0);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        List<ItemChartDayDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entry : map.entrySet()) {
            int in = entry.getValue()[0];
            int out = entry.getValue()[1];
            int inH = max > 0 ? (int) Math.round(in * 100.0 / max) : 0;
            int outH = max > 0 ? (int) Math.round(out * 100.0 / max) : 0;
            result.add(new ItemChartDayDto(entry.getKey().format(fmt), in, out, inH, outH));
        }
        return result;
    }

    public List<ItemListDto> getItemList(String category) {
        if (category == null || category.isBlank()) {
            return itemRepository.findAllByOrderByNameAsc().stream()
                    .map(ItemListDto::new).toList();
        }
        try {
            ItemCategory cat = ItemCategory.valueOf(category);
            return itemRepository.findByCategoryOrderByNameAsc(cat).stream()
                    .map(ItemListDto::new).toList();
        } catch (IllegalArgumentException e) {
            return itemRepository.findAllByOrderByNameAsc().stream()
                    .map(ItemListDto::new).toList();
        }
    }

    public List<ItemCategoryFilter> getCategoryFilters(String selected) {
        String sel = selected == null ? "" : selected;
        return List.of(
                new ItemCategoryFilter("전체", "", sel.isEmpty(), "/item-manager/item-list"),
                new ItemCategoryFilter("의료소모품", "MEDICAL_SUPPLIES",
                        "MEDICAL_SUPPLIES".equals(sel), "/item-manager/item-list?category=MEDICAL_SUPPLIES"),
                new ItemCategoryFilter("의료기기", "MEDICAL_EQUIPMENT",
                        "MEDICAL_EQUIPMENT".equals(sel), "/item-manager/item-list?category=MEDICAL_EQUIPMENT"),
                new ItemCategoryFilter("사무/비품", "GENERAL_SUPPLIES",
                        "GENERAL_SUPPLIES".equals(sel), "/item-manager/item-list?category=GENERAL_SUPPLIES")
        );
    }

    public ItemFormDto getItemForm(Long id) {
        if (id == null) return new ItemFormDto();
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
        return new ItemFormDto(item);
    }

    @Transactional
    public void saveItem(Long id, String name, String category, int quantity, int minQuantity) {
        ItemCategory cat;
        try {
            cat = ItemCategory.valueOf(category);
        } catch (IllegalArgumentException e) {
            throw CustomException.badRequest("VALIDATION_ERROR", "유효하지 않은 카테고리입니다: " + category);
        }
        if (id == null) {
            itemRepository.save(Item.create(name, cat, quantity, minQuantity));
        } else {
            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
            item.update(name, cat, quantity, minQuantity);
        }
    }

    private String getCurrentActorName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "시스템";
        return staffRepository.findByUsernameAndActiveTrue(auth.getName())
                .map(s -> s.getName())
                .orElse(auth.getName());
    }

    @Transactional
    public void restockItem(Long id, int amount) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
        item.addStock(amount);
        stockLogRepository.save(ItemStockLog.of(id, item.getName(), ItemStockType.IN, amount, getCurrentActorName()));
    }

    @Transactional
    public int useItem(Long id, int amount, Long reservationId) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
        int newQuantity = item.getQuantity() - amount;
        if (newQuantity < 0) {
            throw CustomException.badRequest("VALIDATION_ERROR", "재고가 " + (-newQuantity) + "개 부족합니다.");
        }
        item.updateQuantity(newQuantity);
        usageLogRepository.save(ItemUsageLog.of(reservationId, id, item.getName(), amount, getCurrentActorName()));
        stockLogRepository.save(ItemStockLog.of(id, item.getName(), ItemStockType.OUT, amount, getCurrentActorName()));
        return newQuantity;
    }

    public List<ItemUsageLogDto> getUsageLogs(Long reservationId) {
        return usageLogRepository.findByReservationIdOrderByUsedAtAsc(reservationId)
                .stream().map(ItemUsageLogDto::new).toList();
    }

    public List<ItemUsageLogDto> getTodayStaffUsageLogs() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return usageLogRepository.findByReservationIdIsNullAndUsedAtBetweenOrderByUsedAtDesc(start, end)
                .stream().map(ItemUsageLogDto::new).toList();
    }

    public List<ItemUsageLogDto> getTodayUsageLogsByUser(String username) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        String actorName = staffRepository.findByUsernameAndActiveTrue(username)
                .map(s -> s.getName())
                .orElse(username);
        return usageLogRepository.findByReservationIdIsNullAndUsedByAndUsedAtBetweenOrderByUsedAtDesc(actorName, start, end)
                .stream().map(ItemUsageLogDto::new).toList();
    }

    @Transactional
    public int restockItemAndGetQuantity(Long id, int amount) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + id));
        item.addStock(amount);
        stockLogRepository.save(ItemStockLog.of(id, item.getName(), ItemStockType.IN, amount, getCurrentActorName()));
        return item.getQuantity();
    }

    public List<ItemStockLogDto> getStockHistory() {
        return stockLogRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ItemStockLogDto::new).toList();
    }

    public long getTotalInAmount() {
        return stockLogRepository.sumAmountByType(ItemStockType.IN);
    }

    public long getTotalOutAmount() {
        return stockLogRepository.sumAmountByType(ItemStockType.OUT);
    }

    @Transactional
    public java.util.Map<String, Object> cancelItemUsage(Long logId) {
        ItemUsageLog log = usageLogRepository.findById(logId)
                .orElseThrow(() -> CustomException.notFound("사용 기록을 찾을 수 없습니다. ID: " + logId));

        Item item = itemRepository.findById(log.getItemId())
                .orElseThrow(() -> CustomException.notFound("물품을 찾을 수 없습니다. ID: " + log.getItemId()));

        // 재고 복구
        item.addStock(log.getAmount());
        stockLogRepository.save(ItemStockLog.of(item.getId(), item.getName(), ItemStockType.IN, log.getAmount(), getCurrentActorName() + "(사용취소)"));

        Long itemId = item.getId();
        int newQuantity = item.getQuantity();

        // 사용 로그 삭제
        usageLogRepository.delete(log);

        return java.util.Map.of("itemId", itemId, "quantity", newQuantity);
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }
}
