package com.prplhd.cloudfilestorage.storage;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import com.prplhd.cloudfilestorage.domain.StorageResource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface Storage {

    StorageResource getResourceInfo(Long userId, ResourcePath path);

    StorageResource uploadFile(Long userId, ResourcePath resourcePath, MultipartFile file);

    StorageResource createDirectory(Long userId, ResourcePath resourcePath);

    List<StorageResource> getDirectoryContents(Long userId, ResourcePath resourcePath);

    void deleteResource(Long userId, ResourcePath resourcePath);
}
