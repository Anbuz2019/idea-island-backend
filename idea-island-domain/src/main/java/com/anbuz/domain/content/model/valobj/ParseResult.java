package com.anbuz.domain.content.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 内容解析结果，负责承载外部链接解析得到的标题、摘要和封面候选信息。
 */
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
