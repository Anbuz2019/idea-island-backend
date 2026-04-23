package com.anbuz.domain.material.service;

/**
 * 自动失效领域服务接口，定义按主题规则扫描并失效过期资料的能力。
 */
public interface IAutoInvalidService {

    void invalidateExpiredMaterials();
}
