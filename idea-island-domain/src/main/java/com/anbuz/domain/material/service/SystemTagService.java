package com.anbuz.domain.material.service;

import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.Completeness;
import com.anbuz.domain.material.model.valobj.ScoreRange;
import com.anbuz.domain.material.repository.IMaterialRepository;
import com.anbuz.types.enums.TagType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemTagService {

    public static final String SYS_SCORE_RANGE = "sys_score_range";
    public static final String SYS_COMPLETENESS = "sys_completeness";

    private final IMaterialRepository materialRepository;

    public void refreshSystemTags(Long materialId, BigDecimal score, String comment) {
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
    }

}
