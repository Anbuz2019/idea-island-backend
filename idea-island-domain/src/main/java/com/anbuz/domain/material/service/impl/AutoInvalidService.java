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

    private static final int INVALID_PURGE_RETENTION_MONTHS = 6;
    private static final int INVALID_PURGE_BATCH_SIZE = 500;

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

    @Override
    @Transactional
    public int purgeExpiredInvalidMaterials() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMonths(INVALID_PURGE_RETENTION_MONTHS);
        int total = 0;
        while (true) {
            List<Material> materials = materialRepository.findInvalidMaterialsBefore(threshold, INVALID_PURGE_BATCH_SIZE);
            if (materials.isEmpty()) {
                break;
            }
            purgeMaterials(materials, now);
            total += materials.size();
            if (materials.size() < INVALID_PURGE_BATCH_SIZE) {
                break;
            }
        }
        log.info("Invalid material purge completed threshold={} count={}", threshold, total);
        return total;
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
            materialRepository.deleteTags(material.getId());
        }
        log.info("Auto invalid batch completed count={} reason={}", materials.size(), reason);
    }

    private void purgeMaterials(List<Material> materials, LocalDateTime now) {
        for (Material material : materials) {
            materialRepository.deletePermanently(material.getId());
            if (!Boolean.TRUE.equals(material.getDeleted())) {
                decrementTopicMaterialCount(material.getTopicId(), now);
            }
        }
    }

    private void decrementTopicMaterialCount(Long topicId, LocalDateTime now) {
        topicRepository.findTopicById(topicId).ifPresent(topic -> {
            int currentCount = topic.getMaterialCount() == null ? 0 : topic.getMaterialCount();
            topic.setMaterialCount(Math.max(currentCount - 1, 0));
            topic.setUpdatedAt(now);
            topicRepository.updateTopic(topic);
        });
    }
}
