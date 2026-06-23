package com.prplhd.cloudfilestorage.service;

import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.exception.InvalidRequestException;
import com.prplhd.cloudfilestorage.mapper.ResourceResponseMapper;
import com.prplhd.cloudfilestorage.minio.MinioStorage;
import com.prplhd.cloudfilestorage.minio.ResolvedResourcePath;
import com.prplhd.cloudfilestorage.minio.ResourcePathResolver;
import com.prplhd.cloudfilestorage.minio.ResourceType;
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

    private final MinioStorage minioStorage;
    private final ResourcePathResolver resourcePathResolver;
    private final ResourceResponseMapper resourceResponseMapper;

    public ResourceResponseDto getResourceInfo(Long userId, String path) {
        ResolvedResourcePath resolvedPath = resourcePathResolver.resolve(path);

        Long resourceSize = minioStorage.getResourceSize(userId, path);

        if (resolvedPath.resourceType() == ResourceType.DIRECTORY) {
            resourceSize = null;
        }

        return resourceResponseMapper.toDto(resolvedPath, resourceSize);
    }

    public List<ResourceResponseDto> uploadResources(Long userId, String path, List<MultipartFile> files) {
        List<ResourceResponseDto> uploadedResources = new ArrayList<>();

        for (MultipartFile file : files) {
            validateFileName(file.getOriginalFilename());
        }

        for (MultipartFile file : files) {
            String resourceFullPath = path + file.getOriginalFilename();
            ResolvedResourcePath resolvedPath = resourcePathResolver.resolve(resourceFullPath);

            minioStorage.uploadResource(userId, resourceFullPath, file);
            uploadedResources.add(resourceResponseMapper.toDto(resolvedPath, file.getSize()));
        }

        return uploadedResources;
    }

    private void validateFileName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidRequestException("File name is required");
        }

        if (!FILE_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidRequestException("File path must not start or end with '/' and must not contain consecutive slashes");
        }
    }
}
