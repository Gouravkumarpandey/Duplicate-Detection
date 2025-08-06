package com.yourname.filededup.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Service for validating uploaded files
 */
@Service
public class FileValidationService {

    @Value("${app.file.allowed-types}")
    private String allowedMimeTypes;

    @Value("${app.file.allowed-extensions}")
    private String allowedExtensions;

    @Value("${app.file.max-size}")
    private long maxFileSize;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".txt", ".pdf", ".docx");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "text/plain",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Validate uploaded file
     * @param file MultipartFile to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        validateFileSize(file);
        validateFileType(file);
        validateFileName(file);
    }

    /**
     * Validate file size
     */
    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("File size %d bytes exceeds maximum allowed size of %d bytes (%.2f MB)", 
                    file.getSize(), maxFileSize, maxFileSize / (1024.0 * 1024.0))
            );
        }

        if (file.getSize() == 0) {
            throw new IllegalArgumentException("File is empty");
        }
    }

    /**
     * Validate file type and extension
     */
    private void validateFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Check file extension
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("File type '%s' not allowed. Allowed types: %s", 
                    extension, ALLOWED_EXTENSIONS)
            );
        }

        // Check MIME type
        String mimeType = file.getContentType();
        if (mimeType != null && !ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException(
                String.format("MIME type '%s' not allowed. Allowed types: %s", 
                    mimeType, ALLOWED_MIME_TYPES)
            );
        }
    }

    /**
     * Validate filename
     */
    private void validateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Check for dangerous characters
        if (originalFilename.contains("..") || 
            originalFilename.contains("/") || 
            originalFilename.contains("\\")) {
            throw new IllegalArgumentException("Filename contains invalid characters");
        }

        // Check filename length
        if (originalFilename.length() > 255) {
            throw new IllegalArgumentException("Filename too long (max 255 characters)");
        }
    }

    /**
     * Check if file type is allowed
     */
    public boolean isAllowedFileType(String filename) {
        if (filename == null) return false;
        String extension = getFileExtension(filename);
        return ALLOWED_EXTENSIONS.contains(extension.toLowerCase());
    }

    /**
     * Check if MIME type is allowed
     */
    public boolean isAllowedMimeType(String mimeType) {
        return mimeType != null && ALLOWED_MIME_TYPES.contains(mimeType);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    /**
     * Get allowed file extensions
     */
    public List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    /**
     * Get allowed MIME types
     */
    public List<String> getAllowedMimeTypes() {
        return ALLOWED_MIME_TYPES;
    }

    /**
     * Get maximum file size
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
}
