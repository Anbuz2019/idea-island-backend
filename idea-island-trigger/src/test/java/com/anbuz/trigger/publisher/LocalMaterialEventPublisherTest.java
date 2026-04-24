package com.anbuz.trigger.publisher;

import com.anbuz.domain.material.model.event.MaterialSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocalMaterialEventPublisher scenarios")
class LocalMaterialEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private LocalMaterialEventPublisher publisher;

    @Test
    @DisplayName("publishes local material submitted event to spring application events")
    void givenMaterialId_whenPublishMaterialSubmitted_thenPublishesLocalEvent() {
        publisher.publishMaterialSubmitted(100L);

        ArgumentCaptor<MaterialSubmittedEvent> captor = ArgumentCaptor.forClass(MaterialSubmittedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new MaterialSubmittedEvent(100L));
    }
}
