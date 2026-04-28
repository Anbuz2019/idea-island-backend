package com.anbuz.trigger.http;

import com.anbuz.api.http.IMaterialController;
import com.anbuz.domain.material.model.aggregate.MaterialAggregate;
import com.anbuz.domain.material.model.entity.Material;
import com.anbuz.domain.material.model.entity.MaterialMeta;
import com.anbuz.domain.material.model.entity.MaterialTag;
import com.anbuz.domain.material.model.valobj.MaterialPageResult;
import com.anbuz.domain.material.model.valobj.MaterialStatusRecord;

/**
 * 资料 API 映射器，负责在领域查询结果和 HTTP 响应模型之间做装配与高亮转换。
 */
final class MaterialApiMapper {

    private MaterialApiMapper() {
    }

    static IMaterialController.MaterialPageResponse toPageResponse(MaterialPageResult pageResult) {
        return new IMaterialController.MaterialPageResponse(
                pageResult.getItems().stream().map(MaterialApiMapper::toDetailResponse).toList(),
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getPageSize()
        );
    }

    static IMaterialController.MaterialPageResponse toSearchPageResponse(MaterialPageResult pageResult, String keyword) {
        return new IMaterialController.MaterialPageResponse(
                pageResult.getItems().stream().map(item -> toDetailResponse(item, keyword)).toList(),
                pageResult.getTotal(),
                pageResult.getPage(),
                pageResult.getPageSize()
        );
    }

    static IMaterialController.MaterialDetailResponse toDetailResponse(MaterialAggregate aggregate) {
        return toDetailResponse(aggregate, null);
    }

    static IMaterialController.MaterialDetailResponse toDetailResponse(MaterialAggregate aggregate, String keyword) {
        return new IMaterialController.MaterialDetailResponse(
                toMaterialResponse(aggregate.getMaterial()),
                toMetaResponse(aggregate.getMeta()),
                aggregate.getTags() == null ? null : aggregate.getTags().stream().map(MaterialApiMapper::toTagResponse).toList(),
                aggregate.getStatusHistory() == null ? null : aggregate.getStatusHistory().stream().map(MaterialApiMapper::toStatusRecordResponse).toList(),
                toHighlightResponse(aggregate.getMaterial(), keyword)
        );
    }

    private static IMaterialController.MaterialResponse toMaterialResponse(Material material) {
        if (material == null) {
            return null;
        }
        return new IMaterialController.MaterialResponse(
                material.getId(),
                material.getUserId(),
                material.getTopicId(),
                material.getMaterialType() == null ? null : material.getMaterialType().getCode(),
                material.getStatus() == null ? null : material.getStatus().getCode(),
                material.getTitle(),
                material.getDescription(),
                material.getRawContent(),
                material.getSourceUrl(),
                material.getFileKey(),
                material.getComment(),
                material.getScore(),
                isUnread(material),
                material.getInvalidReason(),
                material.getInboxAt(),
                material.getInboxReadAt(),
                material.getCollectedAt(),
                material.getCollectedReadAt(),
                material.getArchivedAt(),
                material.getInvalidAt(),
                material.getLastRetrievedAt(),
                material.getDeleted(),
                material.getDeletedAt(),
                material.getCreatedAt(),
                material.getUpdatedAt()
        );
    }

    private static IMaterialController.MaterialMetaResponse toMetaResponse(MaterialMeta meta) {
        if (meta == null) {
            return null;
        }
        return new IMaterialController.MaterialMetaResponse(
                meta.getId(),
                meta.getMaterialId(),
                meta.getAuthor(),
                meta.getSourcePlatform(),
                meta.getPublishTime(),
                meta.getWordCount(),
                meta.getDurationSeconds(),
                meta.getThumbnailKey(),
                meta.getExtraJson(),
                meta.getCreatedAt(),
                meta.getUpdatedAt()
        );
    }

    private static IMaterialController.MaterialTagResponse toTagResponse(MaterialTag tag) {
        return new IMaterialController.MaterialTagResponse(
                tag.getId(),
                tag.getMaterialId(),
                tag.getTagType() == null ? null : tag.getTagType().getCode(),
                tag.getApiTagGroupKey(),
                tag.getTagValue(),
                tag.getCreatedAt()
        );
    }

    private static IMaterialController.MaterialStatusRecordResponse toStatusRecordResponse(MaterialStatusRecord record) {
        return new IMaterialController.MaterialStatusRecordResponse(record.getStatus(), record.getLabel(), record.getOccurredAt());
    }

    private static IMaterialController.SearchHighlightResponse toHighlightResponse(Material material, String keyword) {
        if (material == null || keyword == null || keyword.isBlank()) {
            return null;
        }

        String highlightedTitle = highlight(material.getTitle(), keyword);
        String highlightedRawContent = highlight(material.getRawContent(), keyword);
        String highlightedComment = highlight(material.getComment(), keyword);
        if (highlightedTitle == null && highlightedRawContent == null && highlightedComment == null) {
            return null;
        }
        return new IMaterialController.SearchHighlightResponse(highlightedTitle, highlightedRawContent, highlightedComment);
    }

    private static String highlight(String value, String keyword) {
        if (value == null || value.isBlank() || keyword == null || keyword.isBlank()) {
            return null;
        }

        String lowerValue = value.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        int fromIndex = 0;
        int matchIndex = lowerValue.indexOf(lowerKeyword, fromIndex);
        if (matchIndex < 0) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        while (matchIndex >= 0) {
            builder.append(escapeHtml(value.substring(fromIndex, matchIndex)));
            builder.append("<em>")
                    .append(escapeHtml(value.substring(matchIndex, matchIndex + keyword.length())))
                    .append("</em>");
            fromIndex = matchIndex + keyword.length();
            matchIndex = lowerValue.indexOf(lowerKeyword, fromIndex);
        }
        builder.append(escapeHtml(value.substring(fromIndex)));
        return builder.toString();
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static boolean isUnread(Material material) {
        if (material == null || material.getStatus() == null) {
            return false;
        }
        return switch (material.getStatus()) {
            case INBOX -> material.getInboxReadAt() == null;
            case COLLECTED -> material.getCollectedReadAt() == null;
            default -> false;
        };
    }
}
