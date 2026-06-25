package com.prplhd.cloudfilestorage.storage.minio;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import com.prplhd.cloudfilestorage.domain.ResourceType;
import com.prplhd.cloudfilestorage.domain.StorageResource;
import com.prplhd.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.prplhd.cloudfilestorage.exception.ResourceNotFoundException;
import com.prplhd.cloudfilestorage.exception.StorageException;
import com.prplhd.cloudfilestorage.storage.Storage;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MinioStorage implements Storage {

    private static final long DIRECTORY_SIZE = 0L;
    private static final String USER_ROOT_PREFIX_TEMPLATE = "user-%d-files/";
    private static final String RESOURCE_NOT_FOUND_CODE = "NoSuchKey";
    private static final String PRECONDITION_FAILED_CODE = "PreconditionFailed";

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public StorageResource getResourceInfo(Long userId, ResourcePath resourcePath) {
        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        try {
            StatObjectResponse statResponse = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            return new StorageResource(resourcePath, statResponse.size());

        } catch (ErrorResponseException e) {
            if (RESOURCE_NOT_FOUND_CODE.equals(e.errorResponse().code())) {
                throw new ResourceNotFoundException("Resource for path '%s' not found".formatted(fullPath));
            }

            throw new StorageException("Failed to get resource info for path '%s'".formatted(fullPath), e);

        } catch (MinioException e) {
            throw new StorageException("Failed to get resource info for path '%s'".formatted(fullPath), e);
        }
    }

    @Override
    public StorageResource uploadFile(Long userId, ResourcePath resourcePath, MultipartFile file) {
        if (resourcePath.getType() != ResourceType.FILE) {
            throw new IllegalArgumentException("Resource path must point to a file");
        }

        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);
        long fileSize = file.getSize();
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        createMissingParentDirectories(userId, resourcePath.getParentDirectories());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, fileSize, -1L)
                            .contentType(contentType)
                            .extraHeaders(Map.of("If-None-Match", "*"))
                            .build()
            );

            return new StorageResource(resourcePath, file.getSize());

        } catch (ErrorResponseException e) {
            if (PRECONDITION_FAILED_CODE.equals(e.errorResponse().code())) {
                throw new ResourceAlreadyExistsException("Resource for path '%s' already exists".formatted(fullPath));
            }

            throw new StorageException("Failed to upload resource for path '%s'".formatted(fullPath), e);

        } catch (IOException | MinioException e) {
            throw new StorageException("Failed to upload resource for path '%s'".formatted(fullPath), e);
        }
    }

    @Override
    public List<StorageResource> getDirectoryContents(Long userId, ResourcePath directoryPath) {
        if (directoryPath.getType() != ResourceType.DIRECTORY) {
            throw new IllegalArgumentException("Resource path must point to a directory");
        }

        validateResourceExists(userId, directoryPath);

        String fullPath = directoryPath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(objectKey)
                        .build());

        return collectStorageResources(userId, results, fullPath);
    }

    @Override
    public StorageResource createDirectory(Long userId, ResourcePath directoryPath) {
        if (directoryPath.getType() != ResourceType.DIRECTORY) {
            throw new IllegalArgumentException("Resource path must point to a directory");
        }

        String fullPath = directoryPath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        validateParentDirectoryExists(userId, directoryPath);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(new byte[]{}), DIRECTORY_SIZE, -1L)
                            .extraHeaders(Map.of("If-None-Match", "*"))
                            .build()
            );

            return new StorageResource(directoryPath, DIRECTORY_SIZE);

        } catch (ErrorResponseException e) {
            if (PRECONDITION_FAILED_CODE.equals(e.errorResponse().code())) {
                throw new ResourceAlreadyExistsException("Directory '%s' already exists".formatted(fullPath));
            }

            throw new StorageException("Failed to create directory '%s'".formatted(fullPath), e);

        } catch (MinioException e) {
            throw new StorageException("Failed to create directory '%s'".formatted(fullPath), e);
        }
    }

    private void createMissingParentDirectories(Long userId, List<String> parentDirectories) {
        for (String parentDirectory : parentDirectories) {
            String objectKey = resolveObjectKey(userId, parentDirectory);

            try {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                                .extraHeaders(Map.of("If-None-Match", "*"))
                                .build()
                );
            } catch (ErrorResponseException e) {
                if (PRECONDITION_FAILED_CODE.equals(e.errorResponse().code())) {
                    continue;
                }

                throw new StorageException("Failed to create parent directory for path '%s'".formatted(parentDirectory), e);

            } catch (MinioException e) {
                throw new StorageException("Failed to create parent directory for path '%s'".formatted(parentDirectory), e);
            }
        }
    }

    private void validateResourceExists(Long userId, ResourcePath resourcePath) {
        getResourceInfo(userId, resourcePath);
    }

    private void validateParentDirectoryExists(Long userId, ResourcePath resourcePath) {
        List<String> parentDirectories = resourcePath.getParentDirectories();

        if (parentDirectories.isEmpty()) {
            return;
        }

        String parentDirectory = parentDirectories.getLast();
        ResourcePath parentDirectoryPath = new ResourcePath(parentDirectory);

        try {
            validateResourceExists(userId, parentDirectoryPath);

        }  catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Parent directory '%s' not found".formatted(parentDirectory));
        }
    }

    private List<StorageResource> collectStorageResources(Long userId, Iterable<Result<Item>> results, String fullPath) {
        try {
            List<StorageResource> resources = new ArrayList<>();
            String userRootPrefix = USER_ROOT_PREFIX_TEMPLATE.formatted(userId);

            for (Result<Item> result : results) {
                Item item = result.get();

                String userRelativePath = item.objectName().substring(userRootPrefix.length());

                if (userRelativePath.equals(fullPath)) {
                    continue;
                }

                ResourcePath resourcePath = new ResourcePath(userRelativePath);
                long resourceSize = item.size();

                resources.add(new StorageResource(resourcePath, resourceSize));
            }

            return resources;

        } catch (MinioException e) {
            throw new StorageException("Failed to get directory contents for path '%s'".formatted(fullPath), e);
        }
    }

    private String resolveObjectKey(Long userId, String fullPath) {
        return USER_ROOT_PREFIX_TEMPLATE.formatted(userId) + fullPath;
    }
}
