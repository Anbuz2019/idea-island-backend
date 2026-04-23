package com.anbuz.domain.content.adapter;

import java.util.Optional;

/**
 * URL 解析适配器接口，负责隔离领域层与网页标题、OG 图片解析能力。
 */
public interface IUrlParserAdapter {

    Optional<String> extractOgImage(String url);

    Optional<String> extractOgTitle(String url);

}
