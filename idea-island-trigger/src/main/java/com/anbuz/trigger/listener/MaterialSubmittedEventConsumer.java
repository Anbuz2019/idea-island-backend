package com.anbuz.trigger.listener;

import com.anbuz.domain.content.service.IContentProcessService;
import com.anbuz.domain.material.model.event.MaterialSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 资料提交事件消费者，负责在应用内异步触发资料内容加工链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialSubmittedEventConsumer {

    private final IContentProcessService contentProcessService;

    @Async("materialEventExecutor")
    @EventListener
    public void onMessage(MaterialSubmittedEvent event) {
        Long materialId = event.materialId();
        log.info("Receive local MaterialSubmittedEvent materialId={}", materialId);
        try {
            contentProcessService.process(materialId);
        } catch (Exception e) {
            log.error("Process MaterialSubmittedEvent failed materialId={}", materialId, e);
            throw e;
        }
    }
}
