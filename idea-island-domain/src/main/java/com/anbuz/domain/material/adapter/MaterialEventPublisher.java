package com.anbuz.domain.material.adapter;

/**
 * 资料事件发布端口，负责把领域事件交给外部消息设施异步处理。
 */
public interface MaterialEventPublisher {

    void publishMaterialSubmitted(Long materialId);

}
