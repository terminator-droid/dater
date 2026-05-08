package com.dudev.datingapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class S3BucketInitializer {

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Bean
    ApplicationRunner initBucket() {
        return args -> {
            try {
                boolean exists = bucketExists();
                if (!exists) {
                    try {
                        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                        log.info("Created S3 bucket '{}'", bucket);
                    } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
                    }
                }
                applyPublicReadPolicy();
            } catch (Exception e) {
                log.warn("S3 bucket initialization failed (bucket='{}'), skipping: {}", bucket, e.getMessage());
            }
        };
    }

    private boolean bucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    private void applyPublicReadPolicy() {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": "s3:GetObject",
                    "Resource": "arn:aws:s3:::%s/*"
                  }]
                }
                """.formatted(bucket);
        try {
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucket)
                    .policy(policy)
                    .build());
        } catch (Exception e) {
            log.warn("Could not apply public-read policy to bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
