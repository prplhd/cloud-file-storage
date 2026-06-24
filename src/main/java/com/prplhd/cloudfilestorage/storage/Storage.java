package com.prplhd.cloudfilestorage.storage;

import org.springframework.web.multipart.MultipartFile;

public interface Storage {

    public Long getResourceSize(Long userId, String path);

    public void uploadResource(Long userId, String path, MultipartFile file);
}
