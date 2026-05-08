package com.dudev.datingapp.user.controller;

import com.dudev.datingapp.user.service.PhotoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Streams uploaded photos through the API server. Clients always reach the
 * same host they used for the API, so there's no network mismatch between
 * Android emulators (10.0.2.2), iOS simulators (localhost), or LAN devices.
 *
 * The S3 endpoint stays internal — only the API has to know how to reach it.
 */
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@Tag(name = "Photos", description = "Public photo CDN proxy")
public class PhotoServingController {

    private final PhotoStorageService storageService;

    @GetMapping("/{userId}/{filename:.+}")
    @Operation(summary = "Stream a stored photo")
    public ResponseEntity<InputStreamResource> servePhoto(
            @PathVariable String userId, @PathVariable String filename) {
        var stream = storageService.fetch(userId + "/" + filename);
        var contentType = stream.contentType() != null
                ? MediaType.parseMediaType(stream.contentType())
                : MediaType.IMAGE_JPEG;
        var builder = ResponseEntity.ok()
                .contentType(contentType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic());
        if (stream.contentLength() != null) {
            builder = builder.contentLength(stream.contentLength());
        }
        return builder.body(new InputStreamResource(stream.body()));
    }
}
