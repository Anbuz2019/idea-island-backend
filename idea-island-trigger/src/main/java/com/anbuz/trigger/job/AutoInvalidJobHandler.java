package com.anbuz.trigger.job;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.topic.model.entity.Topic;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.enums.MaterialStatus;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoInvalidJobHandler {

    private final ITopicRepository topicRepository;
    private final IMaterialRepository materialRepository;

    @XxlJob("autoInvalidJobHandler")
    public void execute() {
        log.info("自动失效任务开始执行");
        // 此处简化：实际应查询所有配置了自动失效规则的主题，按规则执行
        // 完整实现需要 topic_auto_invalid_rule 的 DAO 查询
        log.info("自动失效任务执行完毕");
    }

    private void invalidateMaterials(List<Material> materials, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (Material m : materials) {
            m.setStatus(MaterialStatus.INVALID);
            m.setInvalidReason(reason);
            m.setInvalidAt(now);
            m.setUpdatedAt(now);
            materialRepository.updateMaterial(m);
        }
        log.info("批量失效 {} 条资料，原因: {}", materials.size(), reason);
    }

}
