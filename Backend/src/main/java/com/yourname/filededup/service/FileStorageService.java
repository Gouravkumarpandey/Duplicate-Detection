package com.yourname.filededup.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Service for handling file storage operations with multiple storage options
 */
@Service
public class FileStorageService {

    // Configurable storage directory
    @Value("${app.file.upload-dir:uploads/}")
    private String uploadDirectory;

    @Value("${app.file.temp-dir:temp/}")
    private String tempDirectory;

    @Value("${app.file.storage.type:LOCAL}")
    private String storageType; // LOCAL, GRIDFS, S3, GOOGLE_CLOUD

    @Autowired(required = false)
    private GridFSStorageService gridFSStorageService;

    // ============ CORE STORAGE OPERATIONS ============

    /**
     * Store file with enhanced validation and multiple storage options
     */
    public FileStorageResult storeFile(MultipartFile file, String uploadedBy) throws IOException {
        // Validate file
        validateFile(file);
        
        // Generate unique filename
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename(), uploadedBy);
        
        // Store based on configured storage type
        switch (storageType.toUpperCase()) {
            case "LOCAL":
                return storeFileLocally(file, uniqueFileName, uploadedBy);
            case "GRIDFS":
                return storeFileInGridFS(file, uniqueFileName, uploadedBy);
            case "S3":
                return storeFileInS3(file, uniqueFileName, uploadedBy);
            case "GOOGLE_CLOUD":
                return storeFileInGoogleCloud(file, uniqueFileName, uploadedBy);
            default:
                return storeFileLocally(file, uniqueFileName, uploadedBy);
        }
    }

    /**
     * Option A: Store file locally in uploads/ directory
     */
    private FileStorageResult storeFileLocally(MultipartFile file, String uniqueFileName, String uploadedBy) 
            throws IOException {
        
        // Create upload directory structure by date and user
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String userPath = sanitizeUserPath(uploadedBy);
        
        Path uploadPath = Paths.get(uploadDirectory, userPath, datePath);
        Files.createDirectories(uploadPath);
        
        // Store the actual file
        Path filePath = uploadPath.resolve(uniqueFileName);
        file.transferTo(filePath.toFile());
        
        // Create backup copy in temp directory for verification
        Path tempPath = Paths.get(tempDirectory);
        Files.createDirectories(tempPath);
        Path backupPath = tempPath.resolve("backup_" + uniqueFileName);
        Files.copy(filePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        
        return new FileStorageResult(
            "LOCAL",
            filePath.toString(),
            backupPath.toString(),
            uniqueFileName,
            file.getOriginalFilename(),
            file.getSize(),
            file.getContentType()
        );
    }

    /**
     * Option B: Store file in MongoDB GridFS
     */
    private FileStorageResult storeFileInGridFS(MultipartFile file, String uniqueFileName, String uploadedBy) 
            throws IOException {
        
        if (gridFSStorageService == null) {
            throw new IllegalStateException("GridFS service not available. Please ensure MongoDB GridFS is properly configured.");
        }
        
        GridFSStorageService.GridFSFileInfo gridFSInfo = gridFSStorageService.storeFileInGridFS(file, uploadedBy);
        
        // Also store a local backup copy
        FileStorageResult localBackup = storeFileLocally(file, "backup_" + uniqueFileName, uploadedBy);
        
        return new FileStorageResult(
            "GRIDFS",
            "gridfs://" + gridFSInfo.getFileId(),
            localBackup.getPrimaryPath(),
            gridFSInfo.getGridFSFileName(),
            gridFSInfo.getOriginalFileName(),
            gridFSInfo.getFileSize(),
            gridFSInfo.getContentType()
        );
    }

    /**
     * Option C: Store file in AWS S3
     * Note: This is a placeholder - requires AWS SDK implementation
     */
    private FileStorageResult storeFileInS3(MultipartFile file, String uniqueFileName, String uploadedBy) 
            throws IOException {
        
        // For now, store locally and mark as S3
        // TODO: Implement actual S3 storage using AWS SDK
        FileStorageResult localResult = storeFileLocally(file, uniqueFileName, uploadedBy);
        localResult.setStorageType("S3");
        
        return localResult;
    }

    /**
     * Store file in Google Cloud Storage
     * Note: This is a placeholder - requires Google Cloud SDK implementation
     */
    private FileStorageResult storeFileInGoogleCloud(MultipartFile file, String uniqueFileName, String uploadedBy) 
            throws IOException {
        
        // For now, store locally and mark as Google Cloud
        // TODO: Implement actual Google Cloud Storage using Google Cloud SDK
        FileStorageResult localResult = storeFileLocally(file, uniqueFileName, uploadedBy);
        localResult.setStorageType("GOOGLE_CLOUD");
        
        return localResult;
    }

    // ============ VALIDATION METHODS ============

    /**
     * Enhanced file validation - Only .txt, .pdf, .docx files
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        // Validate file type
        if (!isValidFileType(contentType, fileName)) {
            throw new IllegalArgumentException(
                "Unsupported file type. Only .txt, .pdf, and .docx files are allowed. " +
                "Received: " + (contentType != null ? contentType : "unknown"));
        }

        // Validate file size (max 100MB)
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 100MB");
        }

        // Validate filename
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        // Check for malicious file names
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
    }

    /**
     * Strict file type validation - Only .txt, .pdf, .docx files
     */
    private boolean isValidFileType(String contentType, String fileName) {
        boolean validByMimeType = false;
        boolean validByExtension = false;

        // Check by MIME type - only allow these three types
        if (contentType != null) {
            validByMimeType = contentType.equals("text/plain") ||
                             contentType.equals("application/pdf") ||
                             contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }

        // Check by file extension - only allow .txt, .pdf, .docx
        if (fileName != null) {
            String extension = fileName.toLowerCase();
            validByExtension = extension.endsWith(".txt") ||
                              extension.endsWith(".pdf") ||
                              extension.endsWith(".docx");
        }

        // Both MIME type and extension must be valid
        return validByMimeType && validByExtension;
    }

    // ============ UTILITY METHODS ============

    /**
     * Generate unique filename to avoid conflicts
     */
    private String generateUniqueFileName(String originalFileName, String uploadedBy) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String sanitizedUser = sanitizeUserPath(uploadedBy);
        
        // Extract file extension
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        return sanitizedUser + "_" + timestamp + "_" + uuid + extension;
    }

    /**
     * Sanitize user path for safe directory creation
     */
    private String sanitizeUserPath(String user) {
        if (user == null) {
            return "anonymous";
        }
        return user.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    /**
     * Delete stored file
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    /**
     * Get file size
     */
    public long getFileSize(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.size(path);
    }

    // ============ RESULT CLASS ============

    /**
     * Result object for file storage operations
     */
    public static class FileStorageResult {
        private String storageType;
        private String primaryPath;
        private String backupPath;
        private String storedFileName;
        private String originalFileName;
        private long fileSize;
        private String contentType;

        public FileStorageResult(String storageType, String primaryPath, String backupPath, 
                               String storedFileName, String originalFileName, long fileSize, String contentType) {
            this.storageType = storageType;
            this.primaryPath = primaryPath;
            this.backupPath = backupPath;
            this.storedFileName = storedFileName;
            this.originalFileName = originalFileName;
            this.fileSize = fileSize;
            this.contentType = contentType;
        }

        // Getters and setters
        public String getStorageType() { return storageType; }
        public void setStorageType(String storageType) { this.storageType = storageType; }
        
        public String getPrimaryPath() { return primaryPath; }
        public void setPrimaryPath(String primaryPath) { this.primaryPath = primaryPath; }
        
        public String getBackupPath() { return backupPath; }
        public void setBackupPath(String backupPath) { this.backupPath = backupPath; }
        
        public String getStoredFileName() { return storedFileName; }
        public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }
        
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }
}
