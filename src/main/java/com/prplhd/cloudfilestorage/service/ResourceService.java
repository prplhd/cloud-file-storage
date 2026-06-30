package com.prplhd.cloudfilestorage.service;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import com.prplhd.cloudfilestorage.domain.ResourceType;
import com.prplhd.cloudfilestorage.domain.StorageResource;
import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.exception.InvalidRequestException;
import com.prplhd.cloudfilestorage.mapper.ResourceResponseMapper;
import com.prplhd.cloudfilestorage.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("^(?!/)(?!.*//).+(?<!/)$");
    private static final Pattern DIRECTORY_NAME_PATTERN = Pattern.compile("^[^/]+$");

    private final Storage storage;
    private final ResourceResponseMapper resourceResponseMapper;

    public ResourceResponseDto getResourceInfo(Long userId, String path) {
        ResourcePath resourcePath = new ResourcePath(path);

        StorageResource resource = storage.getResourceInfo(userId, resourcePath);

        return resourceResponseMapper.toDto(resource);
    }

    public List<ResourceResponseDto> uploadResources(Long userId, String path, List<MultipartFile> files) {
        List<ResourceResponseDto> uploadedResources = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFileName(file.getOriginalFilename());
        }

        for (MultipartFile file : files) {
            String resourceFullPath = path + file.getOriginalFilename();
            ResourcePath resourcePath = new ResourcePath(resourceFullPath);

            StorageResource resource = storage.uploadFile(userId, resourcePath, file);

            uploadedResources.add(resourceResponseMapper.toDto(resource));
        }

        return uploadedResources;
    }

    public List<ResourceResponseDto> getDirectoryContents(Long userId, String path) {
        ResourcePath resourcePath = new ResourcePath(path);
        validateDirectoryName(resourcePath.getName());

        List<StorageResource> resources = storage.getDirectoryContents(userId, resourcePath);

        return resources.stream().map(resourceResponseMapper::toDto).toList();
    }

    public ResourceResponseDto createDirectory(Long userId, String path) {
        ResourcePath resourcePath = new ResourcePath(path);
        validateDirectoryName(resourcePath.getName());

        StorageResource resource = storage.createDirectory(userId, resourcePath);

        return resourceResponseMapper.toDto(resource);
    }

    public void deleteResource(Long userId, String path) {
        ResourcePath resourcePath = new ResourcePath(path);

        storage.deleteResource(userId, resourcePath);
    }

    public List<ResourceResponseDto> searchResources(Long userId, String query) {
        List<StorageResource> resources = storage.searchResources(userId, query);

        return resources.stream().map(resourceResponseMapper::toDto).toList();
    }

    public ResourceResponseDto moveResource(Long userId, String sourcePath, String targetPath) {
        ResourcePath sourceResourcePath = new ResourcePath(sourcePath);
        ResourcePath targetResourcePath = new ResourcePath(targetPath);

        validateMoveOperation(sourceResourcePath, targetResourcePath);

        StorageResource resource = storage.moveResource(userId, sourceResourcePath, targetResourcePath);

        return resourceResponseMapper.toDto(resource);
    }

    public void downloadResource(Long userId, String path, OutputStream outputStream) throws IOException {
        ResourcePath resourcePath = new ResourcePath(path);

        switch (resourcePath.getType()) {
            case FILE -> storage.streamResourceTo(userId, resourcePath, outputStream);

            case DIRECTORY -> downloadDirectoryAsZip(userId, path, outputStream);
        }
    }

    private void downloadDirectoryAsZip(Long userId, String path, OutputStream outputStream) throws IOException {
        ResourcePath directoryPath = new ResourcePath(path);
        int directoryPathLength = directoryPath.getFullPath().length();

        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        List<StorageResource> resources = storage.getDirectoryContentsRecursively(userId, directoryPath);

        for (StorageResource resource : resources) {
            String resourceFullPath = resource.getPath().getFullPath();
            String zipEntryName = resourceFullPath.substring(directoryPathLength);

            ZipEntry zipEntry = new ZipEntry(zipEntryName);
            zipOutputStream.putNextEntry(zipEntry);

            if (resource.getType() == ResourceType.FILE) {
                storage.streamResourceTo(userId, resource.getPath(), zipOutputStream);
            }

            zipOutputStream.closeEntry();
        }

        zipOutputStream.finish();
    }

    private void validateMoveOperation(ResourcePath sourceResourcePath, ResourcePath targetResourcePath) {
        String sourceFullPath = sourceResourcePath.getFullPath();
        ResourceType sourceResourceType = sourceResourcePath.getType();

        String targetFullPath = targetResourcePath.getFullPath();
        ResourceType targetResourceType = targetResourcePath.getType();


        if (sourceResourceType != targetResourceType) {
            throw new InvalidRequestException("Source and target paths must refer to resources of the same type");
        }

        if (sourceFullPath.equals(targetFullPath)) {
            throw new InvalidRequestException("Source and target paths must be different");

        }

        if (sourceResourceType == ResourceType.DIRECTORY && targetFullPath.startsWith(sourceFullPath)) {
            throw new InvalidRequestException("A directory cannot be moved into itself or one of its subdirectories");
        }
    }

    private void validateFileName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("File name is required");
        }

        if (!FILE_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidRequestException("File path must not start or end with '/' and must not contain consecutive slashes");
        }
    }

    private void validateDirectoryName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("Directory name is required");
        }

        if (!DIRECTORY_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidRequestException("Directory name must not contain '/'");
        }
    }
}
