package com.anbuz.infrastructure.cos;

import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class JsoupUrlParserAdapter implements IUrlParserAdapter {

    private static final int TIMEOUT_MS = 10_000;

    @Override
    public Optional<String> extractOgImage(String url) {
        return extractOgMeta(url, "og:image");
    }

    @Override
    public Optional<String> extractOgTitle(String url) {
        return extractOgMeta(url, "og:title");
    }

    private Optional<String> extractOgMeta(String url, String property) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent("Mozilla/5.0")
                    .get();
            String content = doc.select("meta[property=" + property + "]").attr("content");
            return content.isBlank() ? Optional.empty() : Optional.of(content);
        } catch (Exception e) {
            log.warn("OG 标签解析失败 url={} property={}: {}", url, property, e.getMessage());
            return Optional.empty();
        }
    }

}
