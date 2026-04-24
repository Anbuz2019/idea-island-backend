package com.anbuz.trigger.publisher;

import com.anbuz.domain.material.adapter.MaterialEventPublisher;
import com.anbuz.domain.material.model.event.MaterialSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 资料事件发布器，负责把领域事件发布为应用内本地异步事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMaterialEventPublisher implements MaterialEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishMaterialSubmitted(Long materialId) {
        log.info("Publish local material submitted event materialId={}", materialId);
        applicationEventPublisher.publishEvent(new MaterialSubmittedEvent(materialId));
    }
}
