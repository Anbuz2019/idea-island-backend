package com.anbuz.domain.content.adapter;

/**
 * 封面存储适配器接口，负责下载外部封面并上传到对象存储。
 */
public interface ICoverStorageAdapter {

    String uploadCover(byte[] imageBytes, String fileName);

    String downloadAndUploadCover(String imageUrl, String fileName);
}
