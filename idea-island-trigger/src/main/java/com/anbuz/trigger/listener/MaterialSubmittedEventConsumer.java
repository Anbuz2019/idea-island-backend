package com.anbuz.trigger.listener;

import com.anbuz.domain.content.service.ContentProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "material-submitted", consumerGroup = "content-process-group")
@RequiredArgsConstructor
public class MaterialSubmittedEventConsumer implements RocketMQListener<String> {

    private final ContentProcessService contentProcessService;

    @Override
    public void onMessage(String message) {
        log.info("收到 MaterialSubmittedEvent: {}", message);
        try {
            Long materialId = Long.parseLong(message.trim());
            contentProcessService.process(materialId);
        } catch (Exception e) {
            log.error("处理 MaterialSubmittedEvent 失败, message={}", message, e);
            throw e;
        }
    }

}
