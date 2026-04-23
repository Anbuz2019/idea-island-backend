package com.anbuz.domain.material.service;

import java.math.BigDecimal;

/**
 * 系统标签领域服务接口，定义评分区间和完整度等系统标签刷新能力。
 */
public interface ISystemTagService {

    void refreshSystemTags(Long materialId, BigDecimal score, String comment);
}
