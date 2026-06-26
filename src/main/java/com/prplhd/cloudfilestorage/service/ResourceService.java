package com.prplhd.cloudfilestorage.service;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import com.prplhd.cloudfilestorage.domain.StorageResource;
import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.exception.InvalidRequestException;
import com.prplhd.cloudfilestorage.mapper.ResourceResponseMapper;
import com.prplhd.cloudfilestorage.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
