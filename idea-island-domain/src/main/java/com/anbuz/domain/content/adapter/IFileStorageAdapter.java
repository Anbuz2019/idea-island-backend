package com.anbuz.domain.content.adapter;

import java.io.InputStream;

/**
 * 文件存储适配器接口，负责隔离领域层与对象存储的上传、删除和访问地址构建能力。
 */
public interface IFileStorageAdapter {

    void upload(String fileKey, InputStream inputStream, long contentLength, String contentType);

    void delete(String fileKey);

    String buildFileUrl(String fileKey);
}
