package com.pharmacy.system.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Validates and securely stores the uploaded prescription file.
     *
     * @param file the uploaded MultipartFile
     * @return the unique relative stored file path / filename
     */
    String storeFile(MultipartFile file);

    /**
     * Loads the stored file as a Spring Resource for viewing/downloading.
     *
     * @param filename the stored filename
     * @return Resource representing the file
     */
    Resource loadFileAsResource(String filename);

    /**
     * Deletes a file if needed.
     *
     * @param filename the filename to delete
     */
    void deleteFile(String filename);
}