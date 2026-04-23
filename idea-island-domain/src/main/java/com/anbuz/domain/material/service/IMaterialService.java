package com.anbuz.domain.material.service;

import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.valobj.MaterialListQuery;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资料领域服务接口，定义资料提交、查询、状态流转、打标和收藏等核心能力。
 */
public interface IMaterialService {

    Long submit(Long userId, SubmitCommand command);

    MaterialPageResult listMaterials(Long userId, MaterialListQuery query);

    MaterialPageResult searchMaterials(Long userId, MaterialListQuery query);

    MaterialPageResult inbox(Long userId, Long topicId, int page, int pageSize);

    MaterialAggregate getDetail(Long userId, Long materialId);

    MaterialAggregate updateBasic(Long userId, Long materialId, UpdateBasicCommand command);

    MaterialAggregate updateMeta(Long userId, Long materialId, UpdateMetaCommand command);

    void deleteMaterial(Long userId, Long materialId);

    void updateTags(Long userId, Long materialId, List<TagInput> tags);

    void markRead(Long userId, Long materialId);

    void collect(Long userId, Long materialId, String comment, BigDecimal score);

    void archive(Long userId, Long materialId);

    void invalidate(Long userId, Long materialId, String invalidReason);

    void restore(Long userId, Long materialId);

    void restoreCollected(Long userId, Long materialId);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SubmitCommand {
        private Long topicId;
        private String materialType;
        private String title;
        private String description;
        private String rawContent;
        private String sourceUrl;
        private String fileKey;
        private String author;
        private String sourcePlatform;
        private LocalDateTime publishTime;
        private Integer durationSeconds;
        private String thumbnailKey;
        private List<TagInput> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class UpdateBasicCommand {
        private String title;
        private String rawContent;
        private String sourceUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class UpdateMetaCommand {
        private String author;
        private String sourcePlatform;
        private LocalDateTime publishTime;
        private Integer wordCount;
        private Integer durationSeconds;
        private String thumbnailKey;
        private String extraJson;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class TagInput {
        private String tagGroupKey;
        private String tagValue;
    }
}
