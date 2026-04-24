package com.anbuz.trigger.listener;

import com.anbuz.domain.content.service.IContentProcessService;
import com.anbuz.domain.material.model.event.MaterialSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("MaterialSubmittedEventConsumer scenarios")
class MaterialSubmittedEventConsumerTest {

    @Mock
    private IContentProcessService contentProcessService;

    @InjectMocks
    private MaterialSubmittedEventConsumer consumer;

    @Test
    @DisplayName("delegates submitted event to content processing")
    void givenMaterialSubmittedEvent_whenOnMessage_thenDelegatesToContentProcessService() {
        consumer.onMessage(new MaterialSubmittedEvent(100L));

        verify(contentProcessService).process(100L);
    }
}
