package com.anbuz.domain.material.model.event;

/**
 * 资料提交领域事件，表示资料落库成功后需要触发后续内容加工。
 */
public record MaterialSubmittedEvent(Long materialId) {
}
