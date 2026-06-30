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
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
    public List<StorageResource> getDirectoryContents(Long userId, ResourcePath directoryPath) {
        return getDirectoryContents(userId, directoryPath, false);
    }

    @Override
    public List<StorageResource> getDirectoryContentsRecursively(Long userId, ResourcePath directoryPath) {
        return getDirectoryContents(userId, directoryPath, true);
    }

    @Override
    public void deleteResource(Long userId, ResourcePath resourcePath) {
        validateResourceExists(userId, resourcePath);

        switch (resourcePath.getType()) {
            case FILE -> deleteFile(userId, resourcePath);
            case DIRECTORY -> deleteDirectoryRecursively(userId, resourcePath);
        }
    }

    @Override
    public List<StorageResource> searchResources(Long userId, String query) {
        String objectKeyRoot = USER_ROOT_PREFIX_TEMPLATE.formatted(userId);
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .recursive(true)
                            .bucket(bucketName)
                            .prefix(objectKeyRoot)
                            .build());

            List<StorageResource> resources = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();
                String objectName = item.objectName();

                String userRelativePath = getUserRelativePath(userId, objectName);
                ResourcePath resourcePath = new ResourcePath(userRelativePath);

                String resourceName = resourcePath.getName();
                String lowerCaseResourceName = resourceName.toLowerCase(Locale.ROOT);

                if (lowerCaseResourceName.contains(lowerCaseQuery)) {
                    resources.add(new StorageResource(resourcePath, item.size()));
                }
            }

            return resources;

        } catch (MinioException e) {
            throw new StorageException("Failed to find resources for query '%s'".formatted(query), e);
        }
    }

    @Override
    public StorageResource moveResource(Long userId, ResourcePath sourceResourcePath, ResourcePath targetResourcePath) {
        ResourceType sourceType = sourceResourcePath.getType();
        ResourceType targetType = targetResourcePath.getType();

        if (sourceType != targetType) {
            throw new IllegalArgumentException("Source and target resource types must match");
        }

        StorageResource sourceResource = getResourceInfo(userId, sourceResourcePath);

        validateParentDirectoryExists(userId, targetResourcePath);
        validateResourceDoesNotExist(userId, targetResourcePath);

        switch (sourceType) {
            case FILE -> moveFile(userId, sourceResourcePath, targetResourcePath);
            case DIRECTORY -> moveDirectory(userId, sourceResourcePath, targetResourcePath);
        }

        return new StorageResource(targetResourcePath, sourceResource.getSize());
    }

    @Override
    public void streamResourceTo(Long userId, ResourcePath resourcePath, OutputStream outputStream) {
        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build())
        ) {
            stream.transferTo(outputStream);

        } catch (MinioException | IOException e) {
            throw new StorageException("Failed to stream resource for path '%s'".formatted(fullPath), e);
        }
    }

    private List<StorageResource> getDirectoryContents(Long userId, ResourcePath directoryPath, boolean recursive) {
        if (directoryPath.getType() != ResourceType.DIRECTORY) {
            throw new IllegalArgumentException("Resource path must point to a directory");
        }

        validateResourceExists(userId, directoryPath);

        String fullPath = directoryPath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .recursive(recursive)
                        .bucket(bucketName)
                        .prefix(objectKey)
                        .build());

        return collectStorageResources(userId, results, fullPath);
    }

    private void moveFile(Long userId, ResourcePath sourceResourcePath, ResourcePath targetResourcePath) {
        String sourceFullPath = sourceResourcePath.getFullPath();
        String sourceObjectKey = resolveObjectKey(userId, sourceFullPath);

        String targetFullPath = targetResourcePath.getFullPath();
        String targetObjectKey = resolveObjectKey(userId, targetFullPath);

        copyObject(userId, sourceObjectKey, targetObjectKey);

        deleteFile(userId, sourceResourcePath);
    }

    private void moveDirectory(Long userId, ResourcePath sourceResourcePath, ResourcePath targetResourcePath) {
        String sourceFullPath = sourceResourcePath.getFullPath();
        String sourcePrefix = resolveObjectKey(userId, sourceFullPath);

        String targetFullPath = targetResourcePath.getFullPath();
        String targetPrefix = resolveObjectKey(userId, targetFullPath);

        Iterable<Result<Item>> sourceObjects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .recursive(true)
                        .bucket(bucketName)
                        .prefix(sourcePrefix)
                        .build());

        for (Result<Item> sourceObject : sourceObjects) {

            try {
                Item item = sourceObject.get();
                String currentSourceKey = item.objectName();

                String relativeSuffix = currentSourceKey.substring(sourcePrefix.length());
                String currentTargetKey = targetPrefix + relativeSuffix;

                copyObject(userId, currentSourceKey, currentTargetKey);

            } catch (MinioException e) {
                throw new StorageException("Failed to move resource from '%s' to '%s'".formatted(sourceFullPath, targetFullPath), e);
            }
        }

        deleteDirectoryRecursively(userId, sourceResourcePath);
    }

    private void copyObject(Long userId, String sourceObjectKey, String targetObjectKey) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(targetObjectKey)
                            .source(
                                    SourceObject.builder()
                                            .bucket(bucketName)
                                            .object(sourceObjectKey)
                                            .build())
                            .extraHeaders(Map.of("If-None-Match", "*"))
                            .build());

        } catch (ErrorResponseException e) {
            if (PRECONDITION_FAILED_CODE.equals(e.errorResponse().code())) {
                throw new ResourceAlreadyExistsException("Resource for target path '%s' already exists"
                        .formatted(getUserRelativePath(userId, targetObjectKey))
                );
            }

            throw new StorageException("Failed to move resource from '%s' to '%s'"
                    .formatted(
                            getUserRelativePath(userId, sourceObjectKey),
                            getUserRelativePath(userId, targetObjectKey)
                    ), e
            );

        } catch (MinioException e) {
            throw new StorageException("Failed to move resource from '%s' to '%s'"
                    .formatted(
                            getUserRelativePath(userId, sourceObjectKey),
                            getUserRelativePath(userId, targetObjectKey)
                    ), e
            );
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

    private void validateResourceDoesNotExist(Long userId, ResourcePath resourcePath) {
        try {
            getResourceInfo(userId, resourcePath);

            String fullPath = resourcePath.getFullPath();

            throw new ResourceAlreadyExistsException("Resource for path '%s' already exists".formatted(fullPath));

        } catch (ResourceNotFoundException ignored) {
        }
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

        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Parent directory '%s' not found".formatted(parentDirectory));
        }
    }

    private List<StorageResource> collectStorageResources(Long userId, Iterable<Result<Item>> results, String fullPath) {
        try {
            List<StorageResource> resources = new ArrayList<>();

            for (Result<Item> result : results) {
                Item item = result.get();

                String userRelativePath = getUserRelativePath(userId, item.objectName());

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

    private void deleteFile(Long userId, ResourcePath resourcePath) {
        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey).build()
            );

        } catch (MinioException e) {
            throw new StorageException("Failed to delete resource for path '%s'".formatted(fullPath), e);
        }
    }

    private void deleteDirectoryRecursively(Long userId, ResourcePath resourcePath) {
        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        List<DeleteRequest.Object> resourcesToDelete = collectResourcesForDeletion(objectKey, fullPath);

        Iterable<Result<DeleteResult.Error>> results =
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder()
                                .bucket(bucketName)
                                .objects(resourcesToDelete)
                                .build()
                );

        List<DeleteResult.Error> deleteErrors = new ArrayList<>();

        try {
            for (Result<DeleteResult.Error> result : results) {
                deleteErrors.add(result.get());
            }

        } catch (MinioException e) {
            throw new StorageException("Failed to delete directory '%s'".formatted(fullPath), e);
        }

        if (!deleteErrors.isEmpty()) {
            String failedResources = deleteErrors.stream()
                    .map(error -> getUserRelativePath(userId, error.objectName()))
                    .collect(Collectors.joining(", "));

            throw new StorageException(
                    "Failed to delete %d resources: %s".formatted(
                            deleteErrors.size(),
                            failedResources
                    )
            );

        }
    }

    private List<DeleteRequest.Object> collectResourcesForDeletion(String objectKey, String fullPath) {
        List<DeleteRequest.Object> resourcesToDelete = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .recursive(true)
                            .bucket(bucketName)
                            .prefix(objectKey)
                            .build());

            for (Result<Item> result : results) {
                Item item = result.get();
                DeleteRequest.Object resourceToDelete = new DeleteRequest.Object(item.objectName());

                resourcesToDelete.add(resourceToDelete);
            }

        } catch (MinioException e) {
            throw new StorageException("Failed to get directory contents for path '%s'".formatted(fullPath), e);
        }

        return resourcesToDelete;
    }

    private String getUserRelativePath(Long userId, String path) {
        String userRootPrefix = USER_ROOT_PREFIX_TEMPLATE.formatted(userId);

        return path.substring(userRootPrefix.length());
    }

    private String resolveObjectKey(Long userId, String fullPath) {
        return USER_ROOT_PREFIX_TEMPLATE.formatted(userId) + fullPath;
    }
}
