package com.dudev.datingapp.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class PhotoStorageService {

    private final Path storageRoot;

    public PhotoStorageService(@Value("${app.storage.path:./uploads}") String storagePath) {
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    public String store(UUID userId, MultipartFile file) throws IOException {
        String ext = resolveExtension(file.getOriginalFilename());
        String key = userId + "/" + UUID.randomUUID() + "." + ext;
        Path target = storageRoot.resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    public void delete(String key) throws IOException {
        Files.deleteIfExists(storageRoot.resolve(key));
    }

    public String toUrl(String key) {
        return "/photos/" + key;
    }

    private String resolveExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
