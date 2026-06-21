package com.prplhd.cloudfilestorage.minio;

import com.prplhd.cloudfilestorage.exception.MinioInitializationException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BucketInitializer implements ApplicationRunner {

    @Value("${minio.bucket.name}")
    private String bucketName;
    private final MinioClient minioClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

                log.info("MinIO bucket {} created", bucketName);
            } else {
                log.info("MinIO bucket {} already exists", bucketName);
            }

        } catch (MinioException e) {
            log.error("Failed to initialize MinIO bucket {}", bucketName, e);

            throw new MinioInitializationException("Failed to initialize storage bucket '" + bucketName + "'", e);
        }
    }
}
