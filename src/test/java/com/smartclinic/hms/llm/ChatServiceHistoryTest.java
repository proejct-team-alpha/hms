package com.smartclinic.hms.llm;

import com.smartclinic.hms.auth.StaffRepository;
import com.smartclinic.hms.domain.ChatbotHistoryRepository;
import com.smartclinic.hms.llm.dto.ChatbotHistoryResponse;
import com.smartclinic.hms.llm.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 히스토리 조회")
class ChatServiceHistoryTest {

    @Mock
    WebClient llmWebClient;

    @Mock
    ChatbotHistoryRepository chatbotHistoryRepository;

    @Mock
    StaffRepository staffRepository;

    @InjectMocks
    ChatService chatService;

    @Test
    @DisplayName("getRuleHistory — Repository 조회를 서비스에서 수행하고 DTO로 매핑")
    void getRuleHistory_delegatesToRepository() {
        // given
        PageRequest pageable = PageRequest.of(0, 20);
        given(chatbotHistoryRepository.findByStaff_IdOrderByCreatedAtDesc(eq(5L), eq(pageable)))
                .willReturn(new PageImpl<>(List.of()));

        // when
        Page<ChatbotHistoryResponse> page = chatService.getRuleHistory(5L, pageable);

        // then
        assertThat(page.getContent()).isEmpty();
        verify(chatbotHistoryRepository).findByStaff_IdOrderByCreatedAtDesc(5L, pageable);
    }
}
