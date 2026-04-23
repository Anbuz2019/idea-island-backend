package com.anbuz.api.http;

import com.anbuz.types.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口契约，定义后端接收上传和按 fileKey 解析地址的 HTTP 边界。
 */
@Tag(name = "文件接口", description = "文件上传与文件地址解析")
@RequestMapping("/api/v1/files")
public interface IFileController {

    @Operation(summary = "上传文件", description = "后端接收 multipart 文件，生成唯一 fileKey 并上传到对象存储")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<UploadResponse> upload(@Valid @ModelAttribute UploadRequest request);

    @Operation(summary = "解析文件地址", description = "根据 fileKey 直接拼出文件访问地址")
    @GetMapping("/resolve")
    Result<FileResponse> resolve(@Valid @ModelAttribute ResolveRequest request);

    @Schema(description = "文件上传请求")
    @Data
    class UploadRequest {
        @Schema(description = "文件二进制内容", type = "string", format = "binary")
        private MultipartFile file;
    }

    @Schema(description = "文件地址解析请求")
    @Data
    class ResolveRequest {
        @Schema(description = "文件唯一标识", example = "6f4b0d4d8f1f4cf6b1d3e6921f25b2b4")
        @NotBlank
        private String fileKey;
    }

    @Schema(description = "文件上传响应")
    record UploadResponse(String fileKey) {}

    @Schema(description = "文件详情响应")
    record FileResponse(String fileKey, String fileUrl) {}
}
