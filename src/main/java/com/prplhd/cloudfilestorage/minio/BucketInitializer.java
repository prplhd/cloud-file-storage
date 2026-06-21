package com.prplhd.cloudfilestorage.minio;

import com.prplhd.cloudfilestorage.exception.MinioInitializationException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

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
            }

        } catch (MinioException e) {
            throw new MinioInitializationException("Failed to initialize storage bucket '" + bucketName + "'", e);
        }
    }
}
