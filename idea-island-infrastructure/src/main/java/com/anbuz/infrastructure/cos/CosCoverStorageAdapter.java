package com.anbuz.infrastructure.cos;

import com.anbuz.domain.content.adapter.ICoverStorageAdapter;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class CosCoverStorageAdapter implements ICoverStorageAdapter {

    private final COSClient cosClient;

    @Value("${cos.bucket}")
    private String bucket;

    @Override
    public String uploadCover(byte[] imageBytes, String fileName) {
        String key = "covers/" + fileName;
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(imageBytes.length);
        meta.setContentType("image/jpeg");
        cosClient.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(imageBytes), meta));
        return key;
    }

    @Override
    public String downloadAndUploadCover(String imageUrl, String fileName) {
        try {
            byte[] bytes = URI.create(imageUrl).toURL().openStream().readAllBytes();
            return uploadCover(bytes, fileName);
        } catch (IOException e) {
            log.warn("下载封面失败: {}", imageUrl, e);
            throw new RuntimeException("封面下载失败", e);
        }
    }

}
