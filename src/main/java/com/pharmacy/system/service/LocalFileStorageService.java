package com.pharmacy.system.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path fileStorageLocation;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "application/pdf"
    );

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    public LocalFileStorageService(@Value("${file.upload-dir:uploads/prescriptions}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Prescription file upload directory initialized at: {}", this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize upload directory: " + this.fileStorageLocation, ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // File cannot be null or empty
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }

        // File size must not exceed 5MB
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 5MB");
        }

        //  Content Type (MIME type) Whitelisting
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file format. Only JPEG, PNG, and PDF files are allowed");
        }

        // Generate unique, safe file name to prevent Path Traversal
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileExtension = originalFilename.substring(extensionIndex).toLowerCase();
        }

        String uniqueFileName = UUID.randomUUID() + fileExtension;

        try {
            // Check if file name contains invalid path traversal characters
            if (uniqueFileName.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence: " + uniqueFileName);
            }

            // Copy file to target location
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("Successfully stored prescription file: {}", uniqueFileName);
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + uniqueFileName + ". Please try again!", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("Prescription document not found: " + filename);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Prescription document not found: " + filename, ex);
        }
    }

    @Override
    public void deleteFile(String filename) {
        try {
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", filename);
        } catch (IOException ex) {
            log.warn("Could not delete file: {}", filename, ex);
        }
    }
}