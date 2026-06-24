package com.prplhd.cloudfilestorage.storage;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import org.springframework.web.multipart.MultipartFile;

public interface Storage {

    public Long getResourceSize(Long userId, ResourcePath path);

    public void uploadFile(Long userId, ResourcePath resourcePath, MultipartFile file);
}
