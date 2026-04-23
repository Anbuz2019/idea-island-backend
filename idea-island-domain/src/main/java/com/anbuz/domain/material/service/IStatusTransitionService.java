package com.anbuz.domain.material.service;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.types.enums.MaterialAction;

import java.math.BigDecimal;
import java.util.List;

/**
 * 状态流转领域服务接口，定义资料动作到状态变更的规则边界。
 */
public interface IStatusTransitionService {

    Material transit(Long materialId, Long currentUserId, MaterialAction action,
                     String comment, BigDecimal score, String invalidReason);

    void assertRequiredTagsFilled(Long materialId, List<Long> requiredGroupIds);
}
