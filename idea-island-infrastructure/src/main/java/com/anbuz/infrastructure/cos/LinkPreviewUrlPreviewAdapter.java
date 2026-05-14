package com.anbuz.infrastructure.cos;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.anbuz.domain.content.adapter.IUrlPreviewAdapter;
import com.anbuz.domain.content.model.UrlPreviewMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class LinkPreviewUrlPreviewAdapter implements IUrlPreviewAdapter {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    @Value("${link-preview.enabled:false}")
    private boolean enabled;

    @Value("${link-preview.api-key:}")
    private String apiKey;

    @Value("${link-preview.endpoint:https://api.linkpreview.net}")
    private String endpoint;

    @Value("${link-preview.timeout-ms:8000}")
    private int timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public Optional<UrlPreviewMetadata> fetchPreview(String url) {
        if (!enabled || !hasText(apiKey) || !hasText(url)) {
            return Optional.empty();
        }
        try {
            URI requestUri = URI.create(normalizeEndpoint(endpoint) + "?q="
                    + URLEncoder.encode(url, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(requestUri)
                    .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000)))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .header("X-Linkpreview-Api-Key", apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("LinkPreview request failed status={} url={}", response.statusCode(), url);
                return Optional.empty();
            }
            JSONObject body = JSON.parseObject(response.body());
            Integer error = body.getInteger("error");
            if (error != null && error == 423) {
                return Optional.of(UrlPreviewMetadata.coverUnavailable("link_preview_robots_forbidden"));
            }
            if (error != null) {
                log.warn("LinkPreview response contains error={} description={} url={}",
                        error, body.getString("description"), url);
                return Optional.empty();
            }
            UrlPreviewMetadata metadata = new UrlPreviewMetadata(
                    text(body.getString("title")),
                    text(body.getString("description")),
                    text(body.getString("image"))
            );
            return metadata.hasAny() ? Optional.of(metadata) : Optional.empty();
        } catch (Exception e) {
            log.warn("LinkPreview request failed url={}: {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeEndpoint(String value) {
        String normalized = hasText(value) ? value.trim() : "https://api.linkpreview.net";
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String text(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
