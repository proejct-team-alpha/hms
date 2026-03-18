package com.smartclinic.hms.item;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.common.exception.CustomException;
import com.smartclinic.hms.domain.Item;
import com.smartclinic.hms.domain.ItemCategory;
import com.smartclinic.hms.item.dto.ItemFormDto;
import com.smartclinic.hms.item.dto.ItemListDto;
import com.smartclinic.hms.item.log.ItemStockLogRepository;
import com.smartclinic.hms.item.log.ItemUsageLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    // ── getItemList ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemList — null 카테고리 입력 시 전체 목록 반환")
    void getItemList_withNullCategory_returnsAllItems() {
        // given
        Item item1 = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 100, 10);
        Item item2 = Item.create("혈압계", ItemCategory.MEDICAL_EQUIPMENT, 5, 2);
        given(itemRepository.findAllByOrderByNameAsc()).willReturn(List.of(item1, item2));

        // when
        List<ItemListDto> result = itemService.getItemList(null);

        // then
        assertThat(result).hasSize(2);
        then(itemRepository).should().findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("getItemList — 유효한 카테고리 입력 시 해당 카테고리 물품만 반환")
    void getItemList_withValidCategory_filtersItems() {
        // given
        Item supplies = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 100, 10);
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
    @DisplayName("getItemList — 존재하지 않는 카테고리 입력 시 전체 목록으로 대체 반환")
    void getItemList_withInvalidCategory_returnsAllItems() {
        // given
        Item item = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 100, 10);
        given(itemRepository.findAllByOrderByNameAsc()).willReturn(List.of(item));

        // when
        List<ItemListDto> result = itemService.getItemList("INVALID_CATEGORY");

        // then
        assertThat(result).hasSize(1);
        then(itemRepository).should().findAllByOrderByNameAsc();
    }

    // ── useItem ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("useItem — 재고 충분 시 차감된 수량 반환")
    void useItem_withSufficientStock_returnsReducedQuantity() {
        // given
        Item item = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 50, 5);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        int result = itemService.useItem(1L, 10, null);

        // then
        assertThat(result).isEqualTo(40);
        then(usageLogRepository).should().save(any());
        then(stockLogRepository).should().save(any());
    }

    @Test
    @DisplayName("useItem — 재고 부족 시 CustomException 발생")
    void useItem_withInsufficientStock_throwsException() {
        // given
        Item item = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 5, 2);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when / then
        assertThatThrownBy(() -> itemService.useItem(1L, 10, null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("재고가");
    }

    // ── restockItem ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("restockItem — 입고 후 재고 증가 및 입고 로그 저장")
    void restockItem_increasesStockAndSavesLog() {
        // given
        Item item = Item.create("주사기", ItemCategory.MEDICAL_SUPPLIES, 20, 5);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        itemService.restockItem(1L, 30);

        // then
        assertThat(item.getQuantity()).isEqualTo(50);
        then(stockLogRepository).should().save(any());
    }

    // ── getItemForm ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getItemForm — null id 입력 시 빈 신규 등록 폼 반환")
    void getItemForm_withNullId_returnsEmptyForm() {
        // given / when
        ItemFormDto form = itemService.getItemForm(null);

        // then
        assertThat(form.isEdit()).isFalse();
        assertThat(form.getName()).isEmpty();
    }

    @Test
    @DisplayName("getItemForm — 존재하는 id 입력 시 해당 물품 정보가 담긴 폼 반환")
    void getItemForm_withExistingId_returnsPopulatedForm() {
        // given
        Item item = Item.create("혈압계", ItemCategory.MEDICAL_EQUIPMENT, 10, 2);
        given(itemRepository.findById(1L)).willReturn(Optional.of(item));

        // when
        ItemFormDto form = itemService.getItemForm(1L);

        // then
        assertThat(form.isEdit()).isTrue();
        assertThat(form.getName()).isEqualTo("혈압계");
        assertThat(form.getCategory()).isEqualTo("MEDICAL_EQUIPMENT");
        assertThat(form.getQuantity()).isEqualTo(10);
    }
}
