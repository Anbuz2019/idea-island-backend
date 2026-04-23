package com.anbuz.domain.content.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

/**
 * 文件服务接口，负责文件上传和基于 fileKey 解析访问地址。
 */
public interface IFileAssetService {

    UploadResult upload(Long userId, UploadCommand command, InputStream inputStream);

    ResolveResult resolve(String fileKey);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class UploadCommand {
        private String fileName;
        private String contentType;
        private Long sizeBytes;
    }

    record UploadResult(String fileKey) {}

    record ResolveResult(String fileKey, String fileUrl) {}
}
