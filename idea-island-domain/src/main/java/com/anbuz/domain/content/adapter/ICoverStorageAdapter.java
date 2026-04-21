package com.anbuz.domain.content.adapter;

public interface ICoverStorageAdapter {

    /** 将图片字节流上传为封面，返回 COS object key */
    String uploadCover(byte[] imageBytes, String fileName);

    /** 将远程图片 URL 下载并上传，返回 COS object key */
    String downloadAndUploadCover(String imageUrl, String fileName);

}
