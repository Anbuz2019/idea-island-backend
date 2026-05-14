package com.anbuz.domain.content.model;

public record UrlPreviewMetadata(String title, String description, String imageUrl, String author,
                                 String sourcePlatform, boolean coverUnavailable, String coverUnavailableReason) {

    public UrlPreviewMetadata(String title, String description, String imageUrl) {
        this(title, description, imageUrl, null, null);
    }

    public UrlPreviewMetadata(String title, String description, String imageUrl, String author,
                              String sourcePlatform) {
        this(title, description, imageUrl, author, sourcePlatform, false, null);
    }

    public static UrlPreviewMetadata coverUnavailable(String reason) {
        return new UrlPreviewMetadata(null, null, null, null, null, true, reason);
    }

    public boolean hasAny() {
        return hasText(title) || hasText(description) || hasText(imageUrl)
                || hasText(author) || hasText(sourcePlatform) || coverUnavailable;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
