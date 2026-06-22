package com.prplhd.cloudfilestorage.service;

import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.exception.MinioStorageException;
import com.prplhd.cloudfilestorage.exception.ResourceNotFoundException;
import com.prplhd.cloudfilestorage.mapper.ResourceResponseMapper;
import com.prplhd.cloudfilestorage.minio.ResolvedResourcePath;
import com.prplhd.cloudfilestorage.minio.ResourcePathResolver;
import com.prplhd.cloudfilestorage.minio.ResourceType;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private static final String RESOURCE_NOT_FOUND_CODE = "NoSuchKey";
    private final MinioClient minioClient;
    private final ResourcePathResolver resourcePathResolver;
    private final ResourceResponseMapper resourceResponseMapper;

    @Value("${minio.bucket.name}")
    private String bucketName;

    public ResourceResponseDto getResourceInfo(Long userId, String path) {
        ResolvedResourcePath resolvedPath = resourcePathResolver.resolve(userId, path);

        String objectKey = resolvedPath.objectKey();

        try {
            StatObjectResponse statResponse = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            Long resourceSize;

            if (resolvedPath.resourceType() == ResourceType.FILE) {
                resourceSize = statResponse.size();
            } else {
                resourceSize = null;
            }

            return resourceResponseMapper.toDto(resolvedPath, resourceSize);

        } catch (ErrorResponseException e) {
            if (RESOURCE_NOT_FOUND_CODE.equals(e.errorResponse().code())) {
                throw new ResourceNotFoundException("Resource for path '%s' not found".formatted(path));
            }

            throw new MinioStorageException("Failed to get resource info for path '%s'".formatted(path), e);

        } catch (MinioException e) {
            throw new MinioStorageException("Failed to get resource info for path '%s'".formatted(path), e);
        }
    }

}
