package com.anbuz.trigger.http;

import com.anbuz.api.http.IFileController;
import com.anbuz.domain.content.service.IFileAssetService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.types.model.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController 文件接口适配")
class FileControllerTest {

    @Mock
    private IFileAssetService fileAssetService;

    @InjectMocks
    private FileController fileController;

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("上传文件时，透传当前用户和文件元数据到领域服务")
        void givenValidRequest_whenUpload_thenDelegatesToDomainService() {
            IFileController.UploadRequest request = new IFileController.UploadRequest();
            request.setFile(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3}));
            UserContext.set(1L);
            try {
                when(fileAssetService.upload(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                        .thenReturn(new IFileAssetService.UploadResult("abc123"));

                Result<IFileController.UploadResponse> result = fileController.upload(request);

                assertThat(result).returns(0, Result::getCode);
                assertThat(result.getData())
                        .returns("abc123", IFileController.UploadResponse::fileKey);
                verify(fileAssetService).upload(org.mockito.ArgumentMatchers.eq(1L), argThat(command ->
                        "cover.png".equals(command.getFileName())
                                && "image/png".equals(command.getContentType())
                                && Long.valueOf(3L).equals(command.getSizeBytes())), org.mockito.ArgumentMatchers.any());
            } finally {
                UserContext.clear();
            }
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("解析文件地址时，返回 fileKey 和 fileUrl")
        void givenFileKey_whenResolve_thenReturnsFileDetail() {
            IFileController.ResolveRequest request = new IFileController.ResolveRequest();
            request.setFileKey("abc123");
            when(fileAssetService.resolve("abc123"))
                    .thenReturn(new IFileAssetService.ResolveResult("abc123", "https://cos.example/abc123"));

            Result<IFileController.FileResponse> result = fileController.resolve(request);

            assertThat(result).returns(0, Result::getCode);
            assertThat(result.getData())
                    .returns("abc123", IFileController.FileResponse::fileKey)
                    .returns("https://cos.example/abc123", IFileController.FileResponse::fileUrl);
        }
    }
}
