package com.anbuz.domain.content.service.impl;

import com.anbuz.domain.content.adapter.IFileStorageAdapter;
import com.anbuz.domain.content.service.IFileAssetService;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 文件服务，负责上传校验、生成唯一 fileKey，并在读取时直接拼接访问地址。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileAssetService implements IFileAssetService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> MEDIA_TYPES = Set.of("video/mp4", "audio/mpeg", "audio/mp4", "video/quicktime");
    private static final long IMAGE_MAX_SIZE = 20L * 1024 * 1024;
    private static final long MEDIA_MAX_SIZE = 500L * 1024 * 1024;

    private final IFileStorageAdapter fileStorageAdapter;

    @Override
    public UploadResult upload(Long userId, UploadCommand command, InputStream inputStream) {
        validate(command);
        String fileKey = UUID.randomUUID().toString().replace("-", "");
        try {
            fileStorageAdapter.upload(fileKey, inputStream, command.getSizeBytes(), command.getContentType());
            log.info("File upload succeeded userId={} fileKey={} contentType={} sizeBytes={}",
                    userId, fileKey, command.getContentType(), command.getSizeBytes());
            return new UploadResult(fileKey);
        } catch (RuntimeException e) {
            deleteQuietly(fileKey);
            throw e;
        }
    }

    @Override
    public ResolveResult resolve(String fileKey) {
        if (fileKey == null || fileKey.isBlank()) {
            throw new AppException(ErrorCode.PARAM_INVALID, "文件标识不能为空");
        }
        return new ResolveResult(fileKey, fileStorageAdapter.buildFileUrl(fileKey));
    }

    private void validate(UploadCommand command) {
        if (command == null) {
            throw new AppException(ErrorCode.PARAM_INVALID, "上传参数不能为空");
        }
        if (command.getFileName() == null || command.getFileName().isBlank()) {
            throw new AppException(ErrorCode.PARAM_INVALID, "文件名不能为空");
        }
        if (command.getContentType() == null || command.getContentType().isBlank()) {
            throw new AppException(ErrorCode.PARAM_INVALID, "文件类型不能为空");
        }
        if (command.getSizeBytes() == null || command.getSizeBytes() <= 0) {
            throw new AppException(ErrorCode.PARAM_INVALID, "文件大小必须大于 0");
        }
        boolean isImage = IMAGE_TYPES.contains(command.getContentType());
        boolean isMedia = MEDIA_TYPES.contains(command.getContentType());
        if (!isImage && !isMedia) {
            log.warn("File upload rejected due to unsupported contentType={}", command.getContentType());
            throw new AppException(ErrorCode.PARAM_INVALID, "文件类型不支持");
        }
        long maxSize = isImage ? IMAGE_MAX_SIZE : MEDIA_MAX_SIZE;
        if (command.getSizeBytes() > maxSize) {
            log.warn("File upload rejected due to oversized file contentType={} sizeBytes={} maxSize={}",
                    command.getContentType(), command.getSizeBytes(), maxSize);
            throw new AppException(ErrorCode.PARAM_INVALID, "文件大小超出限制");
        }
    }

    private void deleteQuietly(String fileKey) {
        try {
            fileStorageAdapter.delete(fileKey);
        } catch (Exception e) {
            log.warn("File rollback delete failed fileKey={}", fileKey, e);
        }
    }
}
