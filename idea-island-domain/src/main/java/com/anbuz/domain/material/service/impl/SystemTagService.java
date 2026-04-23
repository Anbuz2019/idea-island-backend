package com.anbuz.domain.material.service.impl;

import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.Completeness;
import com.anbuz.domain.material.model.valobj.ScoreRange;
import com.anbuz.domain.material.model.valobj.SystemTagDefinition;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.domain.material.service.ISystemTagService;
import com.anbuz.types.enums.TagType;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统标签领域服务，负责根据评分、标签和评论重算资料系统标签。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemTagService implements ISystemTagService {

    public static final String SYS_SCORE_RANGE = SystemTagDefinition.SYS_SCORE_RANGE;
    public static final String SYS_COMPLETENESS = SystemTagDefinition.SYS_COMPLETENESS;

    private final IMaterialRepository materialRepository;

    @Override
    public void refreshSystemTags(Long materialId, BigDecimal score, String comment) {
        if (score == null || comment == null) {
            var material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "资料不存在: " + materialId));
            if (score == null) {
                score = material.getScore();
            }
            if (comment == null) {
                comment = material.getComment();
            }
        }
        List<MaterialTag> userTags = materialRepository.findTagsByMaterialIdAndType(materialId, TagType.USER);
        List<String> userTagValues = userTags.stream().map(MaterialTag::getTagValue).collect(Collectors.toList());

        materialRepository.deleteTagByMaterialIdAndGroupKey(materialId, SYS_SCORE_RANGE);
        materialRepository.deleteTagByMaterialIdAndGroupKey(materialId, SYS_COMPLETENESS);

        LocalDateTime now = LocalDateTime.now();
        ScoreRange scoreRange = ScoreRange.of(score);
        materialRepository.saveTags(List.of(
                MaterialTag.builder()
                        .materialId(materialId)
                        .tagType(TagType.SYSTEM)
                        .tagGroupKey(SYS_SCORE_RANGE)
                        .tagValue(scoreRange.getLabel())
                        .createdAt(now)
                        .build()
        ));

        Completeness completeness = Completeness.of(userTagValues, comment, score);
        materialRepository.saveTags(List.of(
                MaterialTag.builder()
                        .materialId(materialId)
                        .tagType(TagType.SYSTEM)
                        .tagGroupKey(SYS_COMPLETENESS)
                        .tagValue(completeness.getLabel())
                        .createdAt(now)
                        .build()
        ));
        log.info("Refresh system tags succeeded materialId={} scoreRange={} completeness={}",
                materialId, scoreRange.getLabel(), completeness.getLabel());
    }

}
