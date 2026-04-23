package com.anbuz.domain.content.service;

/**
 * 内容加工领域服务接口，定义资料提交后的标题和封面补全能力。
 */
public interface IContentProcessService {

    void process(Long materialId);
}
