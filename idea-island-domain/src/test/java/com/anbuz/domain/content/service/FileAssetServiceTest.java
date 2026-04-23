package com.anbuz.domain.content.service;

import com.anbuz.domain.content.adapter.IFileStorageAdapter;
import com.anbuz.domain.content.service.impl.FileAssetService;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileAssetService 文件服务")
class FileAssetServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private IFileStorageAdapter fileStorageAdapter;

    @InjectMocks
    private FileAssetService fileAssetService;

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("图片文件符合白名单和大小限制时，生成唯一 fileKey 并上传")
        void givenValidImageRequest_whenUpload_thenReturnsGeneratedFileKey() {
            InputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});

            IFileAssetService.UploadResult result = fileAssetService.upload(USER_ID,
                    IFileAssetService.UploadCommand.builder()
                            .fileName("cover.png")
                            .contentType("image/png")
                            .sizeBytes(3L)
                            .build(),
                    inputStream);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(fileStorageAdapter).upload(keyCaptor.capture(), any(InputStream.class), anyLong(), anyString());
            assertThat(result.fileKey()).isEqualTo(keyCaptor.getValue());
            assertThat(result.fileKey()).isNotBlank().doesNotContain("/");
        }

        @Test
        @DisplayName("对象存储上传后若后续流程失败，则补偿删除已上传对象")
        void givenUploadFailure_whenUpload_thenDeletesUploadedObject() {
            org.mockito.Mockito.doThrow(new RuntimeException("cos error"))
                    .when(fileStorageAdapter).upload(anyString(), any(InputStream.class), anyLong(), anyString());

            assertThatThrownBy(() -> fileAssetService.upload(USER_ID,
                    IFileAssetService.UploadCommand.builder()
                            .fileName("cover.png")
                            .contentType("image/png")
                            .sizeBytes(3L)
                            .build(),
                    new ByteArrayInputStream(new byte[]{1, 2, 3})))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("cos error");
        }

        @Test
        @DisplayName("文件类型不在白名单时，拒绝上传")
        void givenUnsupportedContentType_whenUpload_thenThrowsParamInvalid() {
            assertThatThrownBy(() -> fileAssetService.upload(USER_ID,
                    IFileAssetService.UploadCommand.builder()
                            .fileName("shell.sh")
                            .contentType("text/x-shellscript")
                            .sizeBytes(100L)
                            .build(),
                    new ByteArrayInputStream(new byte[]{1})))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());

            verifyNoInteractions(fileStorageAdapter);
        }

        @Test
        @DisplayName("图片超过 20MB 时，拒绝上传")
        void givenOversizedImage_whenUpload_thenThrowsParamInvalid() {
            assertThatThrownBy(() -> fileAssetService.upload(USER_ID,
                    IFileAssetService.UploadCommand.builder()
                            .fileName("big.png")
                            .contentType("image/png")
                            .sizeBytes(20L * 1024 * 1024 + 1)
                            .build(),
                    new ByteArrayInputStream(new byte[]{1})))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("根据 fileKey 直接拼出对象访问地址")
        void givenFileKey_whenResolve_thenReturnsBuiltFileUrl() {
            org.mockito.Mockito.when(fileStorageAdapter.buildFileUrl("abc123"))
                    .thenReturn("https://cos.example/abc123");

            IFileAssetService.ResolveResult result = fileAssetService.resolve("abc123");

            assertThat(result)
                    .returns("abc123", IFileAssetService.ResolveResult::fileKey)
                    .returns("https://cos.example/abc123", IFileAssetService.ResolveResult::fileUrl);
        }

        @Test
        @DisplayName("fileKey 为空时，拒绝解析")
        void givenBlankFileKey_whenResolve_thenThrowsParamInvalid() {
            assertThatThrownBy(() -> fileAssetService.resolve(" "))
                    .isInstanceOf(AppException.class)
                    .extracting("code")
                    .isEqualTo(ErrorCode.PARAM_INVALID.getCode());
        }
    }
}
