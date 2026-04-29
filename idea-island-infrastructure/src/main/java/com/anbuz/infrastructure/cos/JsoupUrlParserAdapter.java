package com.anbuz.infrastructure.cos;

import com.anbuz.domain.content.adapter.IUrlParserAdapter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class JsoupUrlParserAdapter implements IUrlParserAdapter {

    private static final int TIMEOUT_MS = 10_000;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final Pattern DIRECT_IMAGE_URL = Pattern.compile("(?i).+\\.(png|jpe?g|webp|gif|avif)(\\?.*)?$");
    private static final Pattern JSON_LD_IMAGE_URL = Pattern.compile(
            "\"(?:image|thumbnailUrl|thumbnail)\"\\s*:\\s*(?:\"(?<single>https?://[^\"]+)\"|"
                    + "\\[\\s*\"(?<array>https?://[^\"]+)\"|"
                    + "\\{[^}]*\"url\"\\s*:\\s*\"(?<object>https?://[^\"]+)\")",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> IMAGE_META_SELECTORS = List.of(
            "meta[property=og:image:secure_url]",
            "meta[property=og:image:url]",
            "meta[property=og:image]",
            "meta[name=og:image]",
            "meta[name=twitter:image]",
            "meta[property=twitter:image]",
            "meta[name=twitter:image:src]",
            "meta[itemprop=image]",
            "link[rel=image_src]",
            "link[rel=preload][as=image]"
    );
    private static final List<String> TITLE_SELECTORS = List.of(
            "meta[property=og:title]",
            "meta[name=twitter:title]",
            "meta[itemprop=headline]",
            "meta[name=title]"
    );

    @Override
    public Optional<String> extractOgImage(String url) {
        if (isDirectImageUrl(url)) {
            return Optional.of(url);
        }
        return fetchDocument(url).flatMap(this::extractBestImage);
    }

    @Override
    public Optional<String> extractOgTitle(String url) {
        return fetchDocument(url).flatMap(this::extractBestTitle);
    }

    private Optional<Document> fetchDocument(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .referrer("https://www.google.com/")
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .get();
            return Optional.of(doc);
        } catch (Exception e) {
            log.warn("URL parse failed url={}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<String> extractBestImage(Document doc) {
        for (String selector : IMAGE_META_SELECTORS) {
            Optional<String> image = firstResolvedUrl(doc, doc.select(selector));
            if (image.isPresent()) {
                return image;
            }
        }
        Optional<String> jsonLdImage = extractJsonLdImage(doc);
        if (jsonLdImage.isPresent()) {
            return jsonLdImage;
        }
        return doc.select("article img[src], main img[src], [role=main] img[src], img[src]")
                .stream()
                .filter(this::isUsefulImage)
                .max(Comparator.comparingInt(this::scoreImage))
                .flatMap(image -> resolvedUrl(doc, image.attr("src")));
    }

    Optional<String> extractBestTitle(Document doc) {
        for (String selector : TITLE_SELECTORS) {
            String content = doc.select(selector).attr("content").trim();
            if (!content.isBlank()) {
                return Optional.of(content);
            }
        }
        String title = doc.title();
        if (title != null && !title.isBlank()) {
            return Optional.of(title.trim());
        }
        String h1 = doc.select("h1").text().trim();
        return h1.isBlank() ? Optional.empty() : Optional.of(h1);
    }

    private Optional<String> firstResolvedUrl(Document doc, Elements elements) {
        for (Element element : elements) {
            String raw = element.hasAttr("content") ? element.attr("content") : element.attr("href");
            Optional<String> url = resolvedUrl(doc, raw);
            if (url.isPresent()) {
                return url;
            }
        }
        return Optional.empty();
    }

    private Optional<String> extractJsonLdImage(Document doc) {
        for (Element script : doc.select("script[type=application/ld+json]")) {
            Matcher matcher = JSON_LD_IMAGE_URL.matcher(script.data());
            if (matcher.find()) {
                Optional<String> url = resolvedUrl(doc, firstPresent(
                        matcher.group("single"),
                        matcher.group("array"),
                        matcher.group("object")));
                if (url.isPresent()) {
                    return url;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolvedUrl(Document doc, String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        String trimmed = rawUrl.trim();
        if (trimmed.startsWith("data:") || trimmed.startsWith("blob:")) {
            return Optional.empty();
        }
        try {
            return Optional.of(URI.create(doc.baseUri()).resolve(trimmed).toString());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean isUsefulImage(Element image) {
        String src = image.attr("src").toLowerCase();
        String alt = image.attr("alt").toLowerCase();
        String clazz = image.className().toLowerCase();
        String combined = src + " " + alt + " " + clazz;
        return !src.isBlank()
                && !src.startsWith("data:")
                && !combined.contains("avatar")
                && !combined.contains("logo")
                && !combined.contains("icon")
                && !combined.contains("sprite")
                && !combined.contains("placeholder")
                && !combined.contains("transparent")
                && !src.endsWith(".svg");
    }

    private int scoreImage(Element image) {
        int width = parsePositiveInt(image.attr("width"));
        int height = parsePositiveInt(image.attr("height"));
        int score = width * height;
        if (score == 0) {
            score = 10_000;
        }
        if (!image.parents().select("article, main, [role=main]").isEmpty()) {
            score += 50_000;
        }
        if (!image.attr("alt").isBlank()) {
            score += 5_000;
        }
        return score;
    }

    private int parsePositiveInt(String value) {
        try {
            int parsed = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            return Math.max(parsed, 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean isDirectImageUrl(String url) {
        return url != null && DIRECT_IMAGE_URL.matcher(url).matches();
    }
}
