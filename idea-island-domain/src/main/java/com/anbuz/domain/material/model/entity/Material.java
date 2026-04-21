package com.anbuz.domain.material.model.entity;

import com.anbuz.types.enums.MaterialStatus;
import com.anbuz.types.enums.MaterialType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Material {

    private Long id;
    private Long userId;
    private Long topicId;
    private MaterialType materialType;
    private MaterialStatus status;
    private String title;
    private String description;
    private String rawContent;
    private String sourceUrl;
    private String fileKey;
    private String comment;
    private BigDecimal score;
    private String invalidReason;
    private LocalDateTime inboxAt;
    private LocalDateTime collectedAt;
    private LocalDateTime archivedAt;
    private LocalDateTime invalidAt;
    private LocalDateTime lastRetrievedAt;
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
