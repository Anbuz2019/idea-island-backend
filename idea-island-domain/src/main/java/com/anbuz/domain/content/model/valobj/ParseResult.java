package com.anbuz.domain.content.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {

    private String title;
    private String coverImageUrl;
    private String author;
    private String sourcePlatform;

}
