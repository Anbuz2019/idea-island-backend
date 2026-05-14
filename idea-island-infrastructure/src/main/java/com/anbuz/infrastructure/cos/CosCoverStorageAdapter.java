package com.anbuz.infrastructure.cos;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IFileStorageAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CosCoverStorageAdapter implements ICoverStorageAdapter {

    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 12_000;
    private static final int MAX_COVER_BYTES = 8 * 1024 * 1024;
    private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/jpeg";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/124.0 Safari/537.36";

    private final IFileStorageAdapter fileStorageAdapter;

    @Override
    public String uploadCover(byte[] imageBytes, String fileName) {
        return uploadCover(imageBytes, fileName, DEFAULT_IMAGE_CONTENT_TYPE);
    }

    private String uploadCover(byte[] imageBytes, String fileName, String contentType) {
        long sizeBytes = imageBytes.length;
        String fileKey = UUID.randomUUID().toString().replace("-", "");
        try {
            fileStorageAdapter.upload(fileKey, new ByteArrayInputStream(imageBytes), sizeBytes, normalizeImageContentType(contentType));
            return fileKey;
        } catch (RuntimeException e) {
            try {
                fileStorageAdapter.delete(fileKey);
            } catch (Exception deleteException) {
                log.warn("Cover rollback delete failed fileKey={}", fileKey, deleteException);
            }
            throw e;
        }
    }

    @Override
    public String downloadAndUploadCover(String imageUrl, String fileName) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(imageUrl).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
            String referer = refererFor(imageUrl);
            if (referer != null) {
                connection.setRequestProperty("Referer", referer);
            }
            int status = connection.getResponseCode();
            if (status >= 400) {
                throw new IOException("remote status " + status);
            }
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.toLowerCase().startsWith("image/")) {
                throw new IOException("remote content is not image: " + contentType);
            }
            byte[] bytes = readLimited(connection.getInputStream());
            return uploadCover(bytes, fileName, contentType);
        } catch (IOException e) {
            log.warn("Download cover failed imageUrl={}", imageUrl, e);
            throw new RuntimeException("Cover download failed", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private byte[] readLimited(InputStream inputStream) throws IOException {
        try (inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_COVER_BYTES) {
                    throw new IOException("cover image is too large");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private String refererFor(String imageUrl) {
        try {
            String host = URI.create(imageUrl).getHost();
            if (host == null) {
                return null;
            }
            String lowerHost = host.toLowerCase();
            if (lowerHost.endsWith("hdslb.com") || lowerHost.endsWith("biliimg.com")) {
                return "https://www.bilibili.com/";
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String normalizeImageContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_IMAGE_CONTENT_TYPE;
        }
        String normalized = contentType.split(";")[0].trim().toLowerCase();
        return normalized.startsWith("image/") ? normalized : DEFAULT_IMAGE_CONTENT_TYPE;
    }
}
