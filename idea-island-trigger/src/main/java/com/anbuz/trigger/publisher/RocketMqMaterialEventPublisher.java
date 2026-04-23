package com.anbuz.trigger.publisher;

import com.anbuz.domain.material.adapter.MaterialEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 资料事件发布器，负责把领域事件转换为 RocketMQ 消息发送到异步处理链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RocketMqMaterialEventPublisher implements MaterialEventPublisher {

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    @Override
    public void publishMaterialSubmitted(Long materialId) {
        send("material-submitted", materialId);
    }

    private void send(String topic, Long materialId) {
        RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
        if (rocketMQTemplate == null) {
            log.warn("RocketMQTemplate 不存在，跳过发送 topic={} materialId={}", topic, materialId);
            return;
        }
        try {
            rocketMQTemplate.convertAndSend(topic, String.valueOf(materialId));
        } catch (Exception e) {
            log.warn("发送 MQ 失败 topic={} materialId={}", topic, materialId, e);
        }
    }

}
