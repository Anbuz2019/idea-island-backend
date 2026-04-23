package com.anbuz.trigger.job;

import com.anbuz.domain.material.service.IAutoInvalidService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自动失效任务处理器，负责定时扫描到期主题规则并驱动资料状态流转。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoInvalidJobHandler {

    private final IAutoInvalidService autoInvalidService;

    @XxlJob("autoInvalidJobHandler")
    public void execute() {
        log.info("自动失效任务开始执行");
        autoInvalidService.invalidateExpiredMaterials();
        log.info("自动失效任务执行完毕");
    }

}
