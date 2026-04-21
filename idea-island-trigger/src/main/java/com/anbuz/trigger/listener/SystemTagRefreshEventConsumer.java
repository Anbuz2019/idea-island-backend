package com.anbuz.trigger.listener;

import com.anbuz.domain.material.service.SystemTagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "system-tag-refresh", consumerGroup = "system-tag-group")
@RequiredArgsConstructor
public class SystemTagRefreshEventConsumer implements RocketMQListener<String> {

    private final SystemTagService systemTagService;

    @Override
    public void onMessage(String message) {
        log.info("收到 SystemTagRefreshEvent: {}", message);
        try {
            Long materialId = Long.parseLong(message.trim());
            systemTagService.refreshSystemTags(materialId, null, null);
        } catch (Exception e) {
            log.error("刷新系统标签失败, message={}", message, e);
            throw e;
        }
    }

}
