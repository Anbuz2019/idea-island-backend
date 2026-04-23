package com.anbuz.infrastructure.cos;

import com.anbuz.domain.content.adapter.IFileStorageAdapter;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * COS 文件存储适配器，负责后端直传对象存储和访问地址构建。
 */
@Component
@RequiredArgsConstructor
public class CosFileStorageAdapter implements IFileStorageAdapter {

    private final COSClient cosClient;

    @Value("${cos.bucket}")
    private String bucket;

    @Value("${cos.base-url:}")
    private String baseUrl;

    @Override
    public void upload(String fileKey, InputStream inputStream, long contentLength, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        metadata.setContentType(contentType);
        cosClient.putObject(new PutObjectRequest(bucket, fileKey, inputStream, metadata));
    }

    @Override
    public void delete(String fileKey) {
        cosClient.deleteObject(bucket, fileKey);
    }

    @Override
    public String buildFileUrl(String fileKey) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return trimTrailingSlash(baseUrl) + "/" + fileKey;
        }
        return cosClient.getObjectUrl(bucket, fileKey).toString();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
