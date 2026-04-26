package com.anbuz.domain.material.service.impl;

import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.IStatusTransitionService;
import com.anbuz.types.enums.MaterialAction;
import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 状态流转领域服务，负责校验资料动作合法性并生成状态变更记录。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusTransitionService implements IStatusTransitionService {

    private final IMaterialRepository materialRepository;

    @Override
    @Transactional
    public Material transit(Long materialId, Long currentUserId, MaterialAction action,
                            String comment, BigDecimal score, String invalidReason) {
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "资料不存在: " + materialId));

        if (!currentUserId.equals(material.getUserId())) {
            log.warn("Material transition rejected due to forbidden access materialId={} action={} operatorUserId={} ownerUserId={}",
                    materialId, action, currentUserId, material.getUserId());
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        MaterialStatus current = material.getStatus();
        LocalDateTime now = LocalDateTime.now();

        switch (action) {
            case MARK_READ -> {
                assertStatus(current, MaterialStatus.INBOX, action);
                material.setStatus(MaterialStatus.PENDING_REVIEW);
            }
            case COLLECT -> {
                if (current != MaterialStatus.INBOX
                        && current != MaterialStatus.PENDING_REVIEW
                        && current != MaterialStatus.COLLECTED
                        && current != MaterialStatus.ARCHIVED) {
                    log.warn("Material collect rejected due to invalid status materialId={} currentStatus={}", materialId, current);
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                            "当前状态 " + current + " 不支持完成评价");
                }
                boolean hasComment = comment != null && !comment.isBlank();
                boolean hasScore = score != null;
                if (!hasComment && !hasScore) {
                    throw new AppException(ErrorCode.PARAM_INVALID, "完成评价需填写评语或评分至少一项");
                }
                if (hasComment) {
                    material.setComment(comment);
                }
                if (hasScore) {
                    material.setScore(score);
                }
                if (current == MaterialStatus.COLLECTED || current == MaterialStatus.ARCHIVED) {
                    // Already settled materials can update review fields without changing their lifecycle state.
                } else if (hasComment && hasScore) {
                    material.setStatus(MaterialStatus.COLLECTED);
                    material.setCollectedAt(now);
                } else {
                    material.setStatus(MaterialStatus.PENDING_REVIEW);
                }
            }
            case ARCHIVE -> {
                if (current != MaterialStatus.PENDING_REVIEW && current != MaterialStatus.COLLECTED) {
                    log.warn("Material archive rejected due to invalid status materialId={} currentStatus={}", materialId, current);
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                            "当前状态 " + current + " 不支持归档");
                }
                material.setStatus(MaterialStatus.ARCHIVED);
                material.setArchivedAt(now);
            }
            case INVALIDATE -> {
                if (current == MaterialStatus.INVALID) {
                    log.warn("Material invalidate rejected due to already invalid materialId={}", materialId);
                    throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION, "资料已处于失效状态");
                }
                if (invalidReason == null || invalidReason.isBlank()) {
                    throw new AppException(ErrorCode.PARAM_INVALID, "标记失效需填写失效原因");
                }
                material.setStatus(MaterialStatus.INVALID);
                material.setInvalidReason(invalidReason);
                material.setInvalidAt(now);
            }
            case RESTORE -> {
                assertStatus(current, MaterialStatus.INVALID, action);
                material.setStatus(MaterialStatus.INBOX);
                material.setInvalidReason(null);
                material.setInvalidAt(null);
                material.setInboxAt(now);
            }
            case RESTORE_COLLECTED -> {
                assertStatus(current, MaterialStatus.ARCHIVED, action);
                material.setStatus(MaterialStatus.COLLECTED);
                material.setArchivedAt(null);
            }
            default -> throw new AppException(ErrorCode.PARAM_INVALID, "未知动作: " + action);
        }

        material.setUpdatedAt(now);
        materialRepository.updateMaterial(material);
        if (action == MaterialAction.RESTORE) {
            materialRepository.clearInvalidation(materialId, now);
        } else if (action == MaterialAction.RESTORE_COLLECTED) {
            materialRepository.clearArchivedAt(materialId, now);
        }
        log.info("Material transition succeeded materialId={} userId={} action={} fromStatus={} toStatus={}",
                materialId, currentUserId, action, current, material.getStatus());
        return material;
    }

    private void assertStatus(MaterialStatus current, MaterialStatus expected, MaterialAction action) {
        if (current != expected) {
            log.warn("Material transition rejected due to invalid status action={} currentStatus={} expectedStatus={}",
                    action, current, expected);
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "动作 " + action + " 要求状态为 " + expected + "，当前状态为 " + current);
        }
    }

    @Override
    public void assertRequiredTagsFilled(Long materialId, List<Long> requiredGroupIds) {
        if (requiredGroupIds == null || requiredGroupIds.isEmpty()) {
            return;
        }
        List<MaterialTag> tags = materialRepository.findTagsByMaterialIdAndType(materialId, TagType.USER);
        for (Long groupId : requiredGroupIds) {
            String groupKey = String.valueOf(groupId);
            boolean filled = tags.stream().anyMatch(t -> groupKey.equals(t.getTagGroupKey()));
            if (!filled) {
                log.warn("Material collect rejected due to missing required tag materialId={} requiredGroupId={}", materialId, groupId);
                throw new AppException(ErrorCode.BUSINESS_CONFLICT,
                        "必填标签组 " + groupId + " 未打标签，无法完成收录");
            }
        }
    }

}
