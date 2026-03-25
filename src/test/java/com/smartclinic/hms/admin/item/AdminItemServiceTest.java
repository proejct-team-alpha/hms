package com.smartclinic.hms.admin.item;

import com.smartclinic.hms.item.ItemManagerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemManagerService itemManagerService;

    @InjectMocks
    private AdminItemService adminItemService;

    @Test
    @DisplayName("restockItem delegates to item manager service so stock log is recorded")
    void restockItem_delegatesToItemManagerService() {
        // given

        // when
        adminItemService.restockItem(1L, 5);

        // then
        then(itemManagerService).should().restockItem(1L, 5);
    }
}