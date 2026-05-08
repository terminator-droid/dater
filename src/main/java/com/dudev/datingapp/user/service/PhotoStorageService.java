package com.dudev.datingapp.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    public String store(UUID userId, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: jpeg, png, webp, gif");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        String key = userId + "/" + UUID.randomUUID() + "." + ext;
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(resolveContentType(ext))
                        .contentLength(file.getSize())
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return key;
    }

    public void delete(String key) throws IOException {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }

    /// Resolves a stored object's public URL — built off the *current* HTTP
    /// request host so phones on the LAN, Android emulators, and any other
    /// client get a URL that resolves back to the same API server. Photos
    /// stream through `/api/v1/photos/{key}` so we don't depend on the S3
    /// endpoint being reachable from the device.
    public String toUrl(String key) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/photos/")
                .path(key)
                .toUriString();
    }

    public ObjectStream fetch(String key) {
        ResponseInputStream<GetObjectResponse> in = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build());
        return new ObjectStream(in, in.response().contentType(),
                in.response().contentLength());
    }

    public record ObjectStream(ResponseInputStream<GetObjectResponse> body,
                                String contentType, Long contentLength) {}

    private String resolveExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String resolveContentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
