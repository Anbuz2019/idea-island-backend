package com.anbuz.infrastructure.cos;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.anbuz.domain.content.adapter.IUrlPreviewAdapter;
import com.anbuz.domain.content.model.UrlPreviewMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BilibiliUrlPreviewAdapter implements IUrlPreviewAdapter {

    private static final String PLATFORM = "哔哩哔哩";
    private static final String VIEW_API = "https://api.bilibili.com/x/web-interface/view";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";
    private static final Pattern BVID_PATTERN = Pattern.compile("(?i)(BV[0-9A-Za-z]{10,})");
    private static final Pattern AID_PATTERN = Pattern.compile("(?i)(?:^|[?&/=])(?:av|aid=)(\\d+)");

    @Value("${bilibili-preview.enabled:true}")
    private boolean enabled;

    @Value("${bilibili-preview.timeout-ms:8000}")
    private int timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public Optional<UrlPreviewMetadata> fetchPreview(String url) {
        if (!enabled || !hasText(url)) {
            return Optional.empty();
        }
        try {
            String normalizedUrl = normalizeUrl(url);
            Optional<String> apiUrl = toApiUrl(normalizedUrl);
            if (apiUrl.isEmpty()) {
                return Optional.empty();
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl.get()))
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000)))
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", "https://www.bilibili.com/")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Bilibili preview request failed status={} url={}", response.statusCode(), normalizedUrl);
                return Optional.empty();
            }
            JSONObject body = JSON.parseObject(response.body());
            if (body.getIntValue("code") != 0) {
                log.warn("Bilibili preview rejected code={} message={} url={}",
                        body.getIntValue("code"), body.getString("message"), normalizedUrl);
                return Optional.empty();
            }
            JSONObject data = body.getJSONObject("data");
            if (data == null) {
                return Optional.empty();
            }
            JSONObject owner = data.getJSONObject("owner");
            UrlPreviewMetadata metadata = new UrlPreviewMetadata(
                    text(data.getString("title")),
                    firstText(data.getString("desc"), data.getString("dynamic")),
                    normalizeImageUrl(firstText(data.getString("pic"), firstFrame(data), seasonCover(data))),
                    owner == null ? null : text(owner.getString("name")),
                    PLATFORM
            );
            if (!hasText(metadata.imageUrl())) {
                return Optional.of(new UrlPreviewMetadata(
                        metadata.title(),
                        metadata.description(),
                        null,
                        metadata.author(),
                        metadata.sourcePlatform(),
                        true,
                        "bilibili_no_cover"
                ));
            }
            return metadata.hasAny() ? Optional.of(metadata) : Optional.empty();
        } catch (Exception e) {
            log.warn("Bilibili preview request failed url={}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    Optional<String> toApiUrl(String url) {
        String decoded = decode(url);
        Optional<String> bvid = firstMatch(BVID_PATTERN, decoded);
        if (bvid.isPresent()) {
            return Optional.of(VIEW_API + "?bvid=" + bvid.get());
        }
        Optional<String> aid = firstMatch(AID_PATTERN, decoded);
        return aid.map(value -> VIEW_API + "?aid=" + value);
    }

    private String normalizeUrl(String url) {
        String trimmed = url.trim();
        URI uri = URI.create(trimmed);
        String host = uri.getHost();
        if (host == null) {
            return trimmed;
        }
        String lowerHost = host.toLowerCase();
        if (!lowerHost.endsWith("b23.tv")) {
            return trimmed;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000)))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.uri().toString();
        } catch (Exception e) {
            log.warn("Bilibili short url resolve failed url={}: {}", trimmed, e.getMessage());
            return trimmed;
        }
    }

    private Optional<String> firstMatch(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return value;
        }
    }

    private String normalizeImageUrl(String value) {
        String imageUrl = text(value);
        if (imageUrl == null) {
            return null;
        }
        if (imageUrl.startsWith("//")) {
            return "https:" + imageUrl;
        }
        return imageUrl;
    }

    private String firstFrame(JSONObject data) {
        JSONArray pages = data.getJSONArray("pages");
        if (pages == null || pages.isEmpty()) {
            return null;
        }
        JSONObject firstPage = pages.getJSONObject(0);
        return firstPage == null ? null : text(firstPage.getString("first_frame"));
    }

    private String seasonCover(JSONObject data) {
        JSONObject season = data.getJSONObject("ugc_season");
        return season == null ? null : text(season.getString("cover"));
    }

    private String firstText(String... values) {
        for (String value : values) {
            String normalized = text(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String text(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
