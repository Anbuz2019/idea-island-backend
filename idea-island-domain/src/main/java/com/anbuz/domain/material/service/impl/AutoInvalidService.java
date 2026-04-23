package com.anbuz.domain.material.service.impl;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.IAutoInvalidService;
import com.anbuz.domain.topic.model.entity.TopicAutoInvalidRule;
import com.anbuz.domain.topic.repository.ITopicRepository;
import com.anbuz.types.enums.AutoInvalidRuleType;
import com.anbuz.types.enums.MaterialStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动失效领域服务，负责按主题规则把超时待处理资料推进到 INVALID。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoInvalidService implements IAutoInvalidService {

    private final ITopicRepository topicRepository;
    private final IMaterialRepository materialRepository;

    @Override
    @Transactional
    public void invalidateExpiredMaterials() {
        List<TopicAutoInvalidRule> rules = topicRepository.findEnabledAutoInvalidRules();
        log.info("Auto invalid scan started ruleCount={}", rules.size());
        for (TopicAutoInvalidRule rule : rules) {
            invalidateByRule(rule, LocalDateTime.now());
        }
        log.info("Auto invalid scan completed ruleCount={}", rules.size());
    }

    private void invalidateByRule(TopicAutoInvalidRule rule, LocalDateTime now) {
        AutoInvalidRuleType ruleType;
        try {
            ruleType = AutoInvalidRuleType.of(rule.getRuleType());
        } catch (IllegalArgumentException e) {
            log.warn("Auto invalid rule skipped because rule type is unknown topicId={} ruleType={}",
                    rule.getTopicId(), rule.getRuleType());
            return;
        }

        LocalDateTime threshold = now.minusDays(rule.getThresholdDays());
        List<Material> materials = switch (ruleType) {
            case INBOX_TIMEOUT -> materialRepository.findByTopicIdAndStatusAndInboxAtBefore(
                    rule.getTopicId(), ruleType.getTargetStatus(), threshold);
            case PENDING_REVIEW_TIMEOUT -> materialRepository.findByTopicIdAndStatusAndUpdatedAtBefore(
                    rule.getTopicId(), ruleType.getTargetStatus(), threshold);
        };
        invalidateMaterials(materials, "系统自动失效：超过 %d 天未处理".formatted(rule.getThresholdDays()), now);
    }

    private void invalidateMaterials(List<Material> materials, String reason, LocalDateTime now) {
        for (Material material : materials) {
            material.setStatus(MaterialStatus.INVALID);
            material.setInvalidReason(reason);
            material.setInvalidAt(now);
            material.setUpdatedAt(now);
            materialRepository.updateMaterial(material);
        }
        log.info("Auto invalid batch completed count={} reason={}", materials.size(), reason);
    }
}
