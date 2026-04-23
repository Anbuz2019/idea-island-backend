package com.anbuz.trigger.listener;

import com.anbuz.domain.content.service.IContentProcessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialSubmittedEventConsumer scenarios")
class MaterialSubmittedEventConsumerTest {

    @Mock
    private IContentProcessService contentProcessService;

    @InjectMocks
    private MaterialSubmittedEventConsumer consumer;

    @Nested
    @DisplayName("on message")
    class OnMessage {

        @Test
        @DisplayName("parses the material id and delegates to content processing")
        void givenMaterialIdMessage_whenOnMessage_thenDelegatesToContentProcessService() {
            consumer.onMessage(" 100 ");

            verify(contentProcessService).process(100L);
        }

        @Test
        @DisplayName("rethrows malformed messages without calling the domain service")
        void givenMalformedMessage_whenOnMessage_thenThrowsAndSkipsDomainService() {
            assertThatThrownBy(() -> consumer.onMessage("oops"))
                    .isInstanceOf(NumberFormatException.class);

            verifyNoInteractions(contentProcessService);
        }
    }
}
