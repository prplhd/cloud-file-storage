package com.prplhd.cloudfilestorage.storage.minio;

import com.prplhd.cloudfilestorage.exception.MinioStorageException;
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

    public Long getResourceSize(Long userId, String path) {
        String objectKey = resolveObjectKey(userId, path);

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
                throw new ResourceNotFoundException("Resource for path '%s' not found".formatted(path));
            }

            throw new MinioStorageException("Failed to get resource info for path '%s'".formatted(path), e);

        } catch (MinioException e) {
            throw new MinioStorageException("Failed to get resource info for path '%s'".formatted(path), e);
        }
    }

    public void uploadResource(Long userId, String path, MultipartFile file) {
        String objectKey = resolveObjectKey(userId, path);
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
                throw new ResourceAlreadyExistsException("Resource for path '%s' already exists".formatted(path));
            }

            throw new MinioStorageException("Failed to upload resource for path '%s'".formatted(path), e);

        } catch (IOException | MinioException e) {
            throw new MinioStorageException("Failed to upload resource for path '%s'".formatted(path), e);
        }
    }

    private String resolveObjectKey(Long userId, String path) {
        return USER_ROOT_PREFIX_TEMPLATE.formatted(userId) + path;
    }
}
