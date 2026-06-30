package com.prplhd.cloudfilestorage.storage;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import com.prplhd.cloudfilestorage.domain.StorageResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

public interface Storage {

    StorageResource getResourceInfo(Long userId, ResourcePath path);

    StorageResource uploadFile(Long userId, ResourcePath resourcePath, MultipartFile file);

    StorageResource createDirectory(Long userId, ResourcePath directoryPath);

    List<StorageResource> getDirectoryContents(Long userId, ResourcePath directoryPath);

    List<StorageResource> getDirectoryContentsRecursively(Long userId, ResourcePath directoryPath);

    void deleteResource(Long userId, ResourcePath resourcePath);

    List<StorageResource> searchResources(Long userId, String query);

    StorageResource moveResource(Long userId, ResourcePath sourceResourcePath, ResourcePath targetResourcePath);

    void streamResourceTo(Long userId, ResourcePath resourcePath, OutputStream outputStream);
}
