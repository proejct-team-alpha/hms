package com.smartclinic.hms.item;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.item.dto.ItemFormDto;
import com.smartclinic.hms.item.dto.ItemListDto;
import com.smartclinic.hms.item.log.ItemStockLog;
import com.smartclinic.hms.item.log.ItemStockLogRepository;
import com.smartclinic.hms.item.log.ItemStockType;
import com.smartclinic.hms.item.log.ItemUsageLog;
import com.smartclinic.hms.item.log.ItemUsageLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ItemManagerServiceTest {

    @Mock
    private ItemManagerRepository itemRepository;
    @Mock
    private ItemUsageLogRepository usageLogRepository;
    @Mock
    private ItemStockLogRepository stockLogRepository;
    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private ItemManagerService itemService;

    @Test
    @DisplayName("getItemList returns all items when category is null")
    void getItemList_withNullCategory_returnsAllItems() {
        // given
        Item item1 = Item.create("Syringe", ItemCategory.MEDICAL_SUPPLIES, 100, 10);
        Item item2 = Item.create("XRay", ItemCategory.MEDICAL_EQUIPMENT, 5, 2);
        given(itemRepository.findAllByOrderByNameAsc()).willReturn(List.of(item1, item2));

        // when
        List<ItemListDto> result = itemService.getItemList(null);

        // then
        assertThat(result).hasSize(2);
        then(itemRepository).should().findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("getItemList filters items when category is valid")
    void getItemList_withValidCategory_filtersItems() {
        // given
        Item supplies = Item.create("Syringe", ItemCategory.MEDICAL_SUPPLIES, 100, 10);
        given(itemRepository.findByCategoryOrderByNameAsc(ItemCategory.MEDICAL_SUPPLIES))
                .willReturn(List.of(supplies));

        // when
        List<ItemListDto> result = itemService.getItemList("MEDICAL_SUPPLIES");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("MEDICAL_SUPPLIES");
        then(itemRepository).should().findByCategoryOrderByNameAsc(ItemCategory.MEDICAL_SUPPLIES);
    }

    @Test
    @DisplayName("getItemList falls back to all items when category is invalid")
    void getItemList_withInvalidCategory_returnsAllItems() {
        // given
        Item item = Item.create("Syringe", ItemCategory.MEDICAL_SUPPLIES, 100, 10);
        given(itemRepository.findAllByOrderByNameAsc()).willReturn(List.of(item));

        // when
        List<ItemListDto> result = itemService.getItemList("INVALID_CATEGORY");

        // then
        assertThat(result).hasSize(1);
        then(itemRepository).should().findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("useItem decreases stock and returns remaining quantity when stock is enough")
    void useItem_withSufficientStock_returnsReducedQuantity() {
        // given
        Item item = Item.create("Syringe", ItemCategory.MEDICAL_SUPPLIES, 50, 5);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        int result = itemService.useItem(1L, 10, null);

        // then
        assertThat(result).isEqualTo(40);
        then(usageLogRepository).should().save(any());
        then(stockLogRepository).should().save(any());
    }

    @Test
    @DisplayName("useItem throws when stock is insufficient")
    void useItem_withInsufficientStock_throwsException() {
        // given
        Item item = Item.create("Syringe", ItemCategory.MEDICAL_SUPPLIES, 5, 2);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        // then
        assertThatThrownBy(() -> itemService.useItem(1L, 10, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("재고");
    }

    @Test
    @DisplayName("restockItem increases stock and saves stock log")
    void restockItem_increasesStockAndSavesLog() {
        // given
        Item item = Item.create("Syringe", ItemCategory.MEDICAL_SUPPLIES, 20, 5);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        itemService.restockItem(1L, 30);

        // then
        assertThat(item.getQuantity()).isEqualTo(50);
        then(stockLogRepository).should().save(any());
    }

    @Test
    @DisplayName("getItemForm returns empty form when id is null")
    void getItemForm_withNullId_returnsEmptyForm() {
        // given

        // when
        ItemFormDto form = itemService.getItemForm(null);

        // then
        assertThat(form.isEdit()).isFalse();
        assertThat(form.getName()).isEmpty();
    }

    @Test
    @DisplayName("getItemForm returns populated form when item exists")
    void getItemForm_withExistingId_returnsPopulatedForm() {
        // given
        Item item = Item.create("XRay", ItemCategory.MEDICAL_EQUIPMENT, 10, 2);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        ItemFormDto form = itemService.getItemForm(1L);

        // then
        assertThat(form.isEdit()).isTrue();
        assertThat(form.getName()).isEqualTo("XRay");
        assertThat(form.getCategory()).isEqualTo("MEDICAL_EQUIPMENT");
        assertThat(form.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("getStockHistory filters stock logs by date range")
    void getStockHistory_filtersByDateRange() {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        ItemStockLog log = ItemStockLog.of(1L, "Syringe", ItemStockType.IN, 15, "Admin");
        ReflectionTestUtils.setField(log, "createdAt", LocalDateTime.of(2026, 3, 20, 10, 30));
        given(stockLogRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay()))
                .willReturn(List.of(log));

        // when
        var result = itemService.getStockHistory(fromDate, toDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemName()).isEqualTo("Syringe");
        then(stockLogRepository).should().findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("getTotalInAmount sums inbound amount within date range")
    void getTotalInAmount_filtersByDateRange() {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        given(stockLogRepository.sumAmountByTypeAndCreatedAtRange(
                ItemStockType.IN,
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay()))
                .willReturn(25L);

        // when
        long result = itemService.getTotalInAmount(fromDate, toDate);

        // then
        assertThat(result).isEqualTo(25L);
        then(stockLogRepository).should().sumAmountByTypeAndCreatedAtRange(
                ItemStockType.IN,
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("getTotalOutAmount sums outbound amount within date range")
    void getTotalOutAmount_filtersByDateRange() {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        given(stockLogRepository.sumAmountByTypeAndCreatedAtRange(
                ItemStockType.OUT,
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay()))
                .willReturn(12L);

        // when
        long result = itemService.getTotalOutAmount(fromDate, toDate);

        // then
        assertThat(result).isEqualTo(12L);
        then(stockLogRepository).should().sumAmountByTypeAndCreatedAtRange(
                ItemStockType.OUT,
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("getStaffUsageLogs filters usage logs by date range")
    void getStaffUsageLogs_filtersByDateRange() {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        ItemUsageLog log = ItemUsageLog.of(null, 1L, "Syringe", 3, "Admin");
        ReflectionTestUtils.setField(log, "usedAt", LocalDateTime.of(2026, 3, 21, 9, 15));
        given(usageLogRepository.findByReservationIdIsNullAndUsedAtGreaterThanEqualAndUsedAtLessThanOrderByUsedAtDesc(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay()))
                .willReturn(List.of(log));

        // when
        var result = itemService.getStaffUsageLogs(fromDate, toDate);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemName()).isEqualTo("Syringe");
        then(usageLogRepository).should().findByReservationIdIsNullAndUsedAtGreaterThanEqualAndUsedAtLessThanOrderByUsedAtDesc(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("getTodayStaffUsageLogs uses today range")
    void getTodayStaffUsageLogs_usesTodayRange() {
        // given
        LocalDate today = LocalDate.now();
        ItemUsageLog log = ItemUsageLog.of(null, 1L, "Syringe", 2, "Admin");
        ReflectionTestUtils.setField(log, "usedAt", today.atTime(10, 0));
        given(usageLogRepository.findByReservationIdIsNullAndUsedAtGreaterThanEqualAndUsedAtLessThanOrderByUsedAtDesc(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()))
                .willReturn(List.of(log));

        // when
        var result = itemService.getTodayStaffUsageLogs();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(2);
        then(usageLogRepository).should().findByReservationIdIsNullAndUsedAtGreaterThanEqualAndUsedAtLessThanOrderByUsedAtDesc(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("getTotalStaffUsageAmount sums usage amount within date range")
    void getTotalStaffUsageAmount_filtersByDateRange() {
        // given
        LocalDate fromDate = LocalDate.of(2026, 3, 20);
        LocalDate toDate = LocalDate.of(2026, 3, 21);
        given(usageLogRepository.sumAmountByReservationIdIsNullAndUsedAtRange(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay()))
                .willReturn(9L);

        // when
        long result = itemService.getTotalStaffUsageAmount(fromDate, toDate);

        // then
        assertThat(result).isEqualTo(9L);
        then(usageLogRepository).should().sumAmountByReservationIdIsNullAndUsedAtRange(
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay());
    }
}
