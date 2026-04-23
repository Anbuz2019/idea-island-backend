package com.anbuz.infrastructure.cos;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.anbuz.domain.content.adapter.IFileStorageAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * COS 封面存储适配器，负责下载远程封面并上传到对象存储。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CosCoverStorageAdapter implements ICoverStorageAdapter {

    private final IFileStorageAdapter fileStorageAdapter;

    @Override
    public String uploadCover(byte[] imageBytes, String fileName) {
        long sizeBytes = imageBytes.length;
        String fileKey = UUID.randomUUID().toString().replace("-", "");
        try {
            fileStorageAdapter.upload(fileKey, new ByteArrayInputStream(imageBytes), sizeBytes, "image/jpeg");
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
        try {
            byte[] bytes = URI.create(imageUrl).toURL().openStream().readAllBytes();
            return uploadCover(bytes, fileName);
        } catch (IOException e) {
            log.warn("Download cover failed imageUrl={}", imageUrl, e);
            throw new RuntimeException("封面下载失败", e);
        }
    }
}
