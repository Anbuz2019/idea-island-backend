package com.anbuz.domain.search.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQuery {

    private Long userId;
    private Long topicId;
    private String keyword;
    private List<String> statuses;
    private List<String> materialTypes;
    private int page;
    private int pageSize;

}
