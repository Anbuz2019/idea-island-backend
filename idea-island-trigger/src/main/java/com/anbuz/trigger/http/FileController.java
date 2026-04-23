package com.anbuz.trigger.http;

import com.anbuz.api.http.IFileController;
import com.anbuz.domain.content.service.IFileAssetService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件 HTTP 适配器，负责接收上传并按 fileKey 解析访问地址。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileController implements IFileController {

    private final IFileAssetService fileAssetService;

    @Override
    public Result<UploadResponse> upload(@Valid @ModelAttribute UploadRequest request) {
        if (request.getFile() == null || request.getFile().isEmpty()) {
            throw new AppException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        Long userId = UserContext.currentUserId();
        log.info("File upload requested userId={} originalFileName={} contentType={} sizeBytes={}",
                userId, request.getFile().getOriginalFilename(),
                request.getFile().getContentType(), request.getFile().getSize());
        try (InputStream inputStream = request.getFile().getInputStream()) {
            IFileAssetService.UploadResult result = fileAssetService.upload(userId,
                    IFileAssetService.UploadCommand.builder()
                            .fileName(request.getFile().getOriginalFilename())
                            .contentType(request.getFile().getContentType())
                            .sizeBytes(request.getFile().getSize())
                            .build(),
                    inputStream);
            log.info("File upload succeeded userId={} fileKey={}", userId, result.fileKey());
            return Result.ok(new UploadResponse(result.fileKey()));
        } catch (IOException e) {
            log.error("File upload failed due to request stream error userId={}", userId, e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    @Override
    public Result<FileResponse> resolve(@Valid @ModelAttribute ResolveRequest request) {
        log.info("Resolve file requested fileKey={}", request.getFileKey());
        IFileAssetService.ResolveResult result = fileAssetService.resolve(request.getFileKey());
        return Result.ok(new FileResponse(result.fileKey(), result.fileUrl()));
    }
}
