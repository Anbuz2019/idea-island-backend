package com.anbuz.test.web;

import com.anbuz.domain.content.service.IFileAssetService;
import com.anbuz.trigger.auth.UserContext;
import com.anbuz.trigger.http.FileController;
import com.anbuz.trigger.http.GlobalExceptionHandler;
import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileController MockMvc scenarios")
class FileControllerWebMvcTest {

    private static final String TEST_USER_HEADER = "X-Test-UserId";

    @Mock
    private IFileAssetService fileAssetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FileController fileController = new FileController(fileAssetService);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(fileController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .addFilters(testUserContextFilter())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("returns fileKey when multipart request is valid")
        void givenValidRequest_whenUpload_thenReturnsFileKey() throws Exception {
            when(fileAssetService.upload(eq(1L), any(), any()))
                    .thenReturn(new IFileAssetService.UploadResult("abc123"));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3}))
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileKey").value("abc123"));
        }

        @Test
        @DisplayName("returns param invalid when file part is missing")
        void givenMissingFile_whenUpload_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(multipart("/api/v1/files/upload")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(fileAssetService);
        }

        @Test
        @DisplayName("returns param invalid when domain service rejects content type")
        void givenUnsupportedContentType_whenUpload_thenReturnsParamInvalid() throws Exception {
            when(fileAssetService.upload(eq(1L), any(), any()))
                    .thenThrow(new AppException(ErrorCode.PARAM_INVALID, "文件类型不支持"));

            mockMvc.perform(multipart("/api/v1/files/upload")
                            .file(new MockMultipartFile("file", "shell.sh", "text/x-shellscript", new byte[]{1}))
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("returns file url when fileKey is valid")
        void givenFileKey_whenResolve_thenReturnsFileUrl() throws Exception {
            when(fileAssetService.resolve("abc123"))
                    .thenReturn(new IFileAssetService.ResolveResult("abc123", "https://cos.example/abc123"));

            mockMvc.perform(get("/api/v1/files/resolve")
                            .param("fileKey", "abc123")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.fileKey").value("abc123"))
                    .andExpect(jsonPath("$.data.fileUrl").value("https://cos.example/abc123"));
        }

        @Test
        @DisplayName("returns param invalid when fileKey is blank")
        void givenBlankFileKey_whenResolve_thenReturnsParamInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/files/resolve")
                            .param("fileKey", "")
                            .header(TEST_USER_HEADER, "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(ErrorCode.PARAM_INVALID.getCode()));

            verifyNoInteractions(fileAssetService);
        }
    }

    private Filter testUserContextFilter() {
        return (request, response, chain) -> {
            String userId = request instanceof jakarta.servlet.http.HttpServletRequest httpServletRequest
                    ? httpServletRequest.getHeader(TEST_USER_HEADER)
                    : null;
            if (userId != null && !userId.isBlank()) {
                UserContext.set(Long.parseLong(userId));
            }
            try {
                chain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
        };
    }
}
