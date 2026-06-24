package com.prplhd.cloudfilestorage.storage.minio;

import com.prplhd.cloudfilestorage.domain.ResourcePath;
import com.prplhd.cloudfilestorage.exception.StorageException;
import com.prplhd.cloudfilestorage.exception.ResourceAlreadyExistsException;
import com.prplhd.cloudfilestorage.exception.ResourceNotFoundException;
import com.prplhd.cloudfilestorage.storage.Storage;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MinioStorage implements Storage {

    private static final String USER_ROOT_PREFIX_TEMPLATE = "user-%d-files/";
    private static final String RESOURCE_NOT_FOUND_CODE = "NoSuchKey";
    private static final String PRECONDITION_FAILED_CODE = "PreconditionFailed";

    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;

    public Long getResourceSize(Long userId, ResourcePath resourcePath) {
        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        try {
            StatObjectResponse statResponse = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            return statResponse.size();

        } catch (ErrorResponseException e) {
            if (RESOURCE_NOT_FOUND_CODE.equals(e.errorResponse().code())) {
                throw new ResourceNotFoundException("Resource for path '%s' not found".formatted(fullPath));
            }

            throw new StorageException("Failed to get resource info for path '%s'".formatted(fullPath), e);

        } catch (MinioException e) {
            throw new StorageException("Failed to get resource info for path '%s'".formatted(fullPath), e);
        }
    }

    public void uploadResource(Long userId, ResourcePath resourcePath, MultipartFile file) {
        String fullPath = resourcePath.getFullPath();
        String objectKey = resolveObjectKey(userId, fullPath);

        long fileSize = file.getSize();
        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

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

        } catch (ErrorResponseException e) {
            if (PRECONDITION_FAILED_CODE.equals(e.errorResponse().code())) {
                throw new ResourceAlreadyExistsException("Resource for path '%s' already exists".formatted(fullPath));
            }

            throw new StorageException("Failed to upload resource for path '%s'".formatted(fullPath), e);

        } catch (IOException | MinioException e) {
            throw new StorageException("Failed to upload resource for path '%s'".formatted(fullPath), e);
        }
    }

    private String resolveObjectKey(Long userId, String fullPath) {
        return USER_ROOT_PREFIX_TEMPLATE.formatted(userId) + fullPath;
    }
}
