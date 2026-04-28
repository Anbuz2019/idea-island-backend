package com.anbuz.infrastructure.persistent.po;

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
public class MaterialPO {

    private Long id;
    private Long userId;
    private Long topicId;
    private String materialType;
    private String status;
    private String title;
    private String description;
    private String rawContent;
    private String sourceUrl;
    private String fileKey;
    private String comment;
    private BigDecimal score;
    private String invalidReason;
    private LocalDateTime inboxAt;
    private LocalDateTime inboxReadAt;
    private LocalDateTime collectedAt;
    private LocalDateTime collectedReadAt;
    private LocalDateTime archivedAt;
    private LocalDateTime invalidAt;
    private LocalDateTime lastRetrievedAt;
    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
