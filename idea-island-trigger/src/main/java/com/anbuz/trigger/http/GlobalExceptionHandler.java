package com.anbuz.trigger.http;

import com.anbuz.types.exception.AppException;
import com.anbuz.types.model.ErrorCode;
import com.anbuz.types.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，负责把 Controller 链路异常转换为统一失败响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public Result<Void> handleAppException(HttpServletRequest request, AppException e) {
        log.warn("Business exception path={} code={} message={}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(HttpServletRequest request, MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Validation exception path={} message={}", request.getRequestURI(), message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(HttpServletRequest request, BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("Bind exception path={} message={}", request.getRequestURI(), message);
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public Result<Void> handleMissingRequestPart(HttpServletRequest request, MissingServletRequestPartException e) {
        log.warn("Missing multipart part path={} partName={}", request.getRequestURI(), e.getRequestPartName());
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), "缺少上传文件");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(HttpServletRequest request, MaxUploadSizeExceededException e) {
        log.warn("Upload size exceeded path={} message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), "文件大小超出限制");
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipart(HttpServletRequest request, MultipartException e) {
        log.warn("Multipart exception path={} message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), "文件上传参数无效");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(HttpServletRequest request, IllegalArgumentException e) {
        log.warn("Illegal argument path={} message={}", request.getRequestURI(), e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID.getCode(), e.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResource(HttpServletRequest request, NoResourceFoundException e) {
        log.debug("No static resource path={} resource={}", request.getRequestURI(), e.getResourcePath());
        return Result.fail(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknown(HttpServletRequest request, Exception e) {
        log.error("Unhandled exception path={}", request.getRequestURI(), e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }

}
