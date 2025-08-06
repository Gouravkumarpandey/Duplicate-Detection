package com.yourname.filededup.service;

import com.yourname.filededup.model.FileModel;
import com.yourname.filededup.dto.FileUploadResponse;
import com.yourname.filededup.repository.FileModelRepository;
import com.yourname.filededup.util.FileHashUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling file uploads with duplicate detection
 */
@Service
public class FileUploadService {

    @Autowired
    private FileModelRepository fileModelRepository;

    @Autowired
    private FileValidationService fileValidationService;

    @Value("${app.file.upload-dir:uploads/}")
    private String uploadDirectory;

    @Value("${app.file.max-size:104857600}") // 100MB default
    private long maxFileSize;

    /**
     * Upload a file with duplicate detection
     */
    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        // Validate file using FileValidationService
        fileValidationService.validateFile(file);
        
        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Calculate SHA-256 hash
        String hash = FileHashUtil.calculateSHA256(file.getInputStream());
        
        // Check for existing file with same hash
        Optional<FileModel> existingFile = fileModelRepository.findByHash(hash);
        boolean isDuplicate = existingFile.isPresent();

        // Generate unique filename to avoid conflicts
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(originalFilename);
        
        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file to local storage
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create FileModel
        FileModel fileModel = new FileModel();
        fileModel.setFilename(uniqueFilename);
        fileModel.setOriginalName(originalFilename);
        fileModel.setFileType(fileExtension);
        fileModel.setHash(hash);
        fileModel.setFileSize(file.getSize());
        fileModel.setMimeType(file.getContentType());
        fileModel.setFilePath(filePath.toString());
        fileModel.setUploadDate(LocalDateTime.now());
        fileModel.setIsDuplicate(isDuplicate);

        // Save to MongoDB
        FileModel savedFile = fileModelRepository.save(fileModel);

        // Create response
        FileUploadResponse response = FileUploadResponse.success(
            isDuplicate ? "File uploaded successfully (duplicate detected)" : "File uploaded successfully",
            isDuplicate,
            savedFile
        );

        // If duplicate, include existing files information
        if (isDuplicate) {
            List<FileModel> duplicates = fileModelRepository.findAllByHash(hash);
            response.setExistingFiles(duplicates);
        }

        return response;
    }

    /**
     * Get all uploaded files
     */
    public List<FileModel> getAllFiles() {
        return fileModelRepository.findAll();
    }

    /**
     * Get all duplicate files
     */
    public List<FileModel> getDuplicateFiles() {
        return fileModelRepository.findByIsDuplicateTrue();
    }

    /**
     * Get all unique files
     */
    public List<FileModel> getUniqueFiles() {
        return fileModelRepository.findByIsDuplicateFalse();
    }

    /**
     * Check if file exists by hash
     */
    public boolean fileExistsByHash(String hash) {
        return fileModelRepository.existsByHash(hash);
    }

    /**
     * Get file statistics
     */
    public Map<String, Object> getFileStatistics() {
        List<FileModel> allFiles = fileModelRepository.findAll();
        long totalFiles = allFiles.size();
        long duplicateCount = fileModelRepository.countByIsDuplicateTrue();
        long uniqueCount = fileModelRepository.countByIsDuplicateFalse();
        long totalSize = allFiles.stream().mapToLong(FileModel::getFileSize).sum();

        return Map.of(
            "totalFiles", totalFiles,
            "duplicateFiles", duplicateCount,
            "uniqueFiles", uniqueCount,
            "totalSizeBytes", totalSize,
            "totalSizeMB", totalSize / (1024.0 * 1024.0),
            "duplicatePercentage", totalFiles > 0 ? (duplicateCount * 100.0 / totalFiles) : 0.0
        );
    }

    /**
     * Delete a file by ID
     */
    public boolean deleteFile(String id) {
        Optional<FileModel> fileOptional = fileModelRepository.findById(id);
        if (fileOptional.isPresent()) {
            FileModel fileModel = fileOptional.get();
            
            // Delete physical file
            try {
                Path filePath = Paths.get(fileModel.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log warning but continue with database deletion
                System.err.println("Warning: Could not delete physical file: " + e.getMessage());
            }
            
            // Delete from database
            fileModelRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get files by hash (all duplicates)
     */
    public List<FileModel> getFilesByHash(String hash) {
        return fileModelRepository.findAllByHash(hash);
    }

    /**
     * Clean up orphaned files
     */
    public Map<String, Object> cleanupOrphanedFiles() {
        int deletedFiles = 0;
        int errors = 0;

        // Get all files from database
        List<FileModel> allFiles = fileModelRepository.findAll();
        
        for (FileModel fileModel : allFiles) {
            Path filePath = Paths.get(fileModel.getFilePath());
            if (!Files.exists(filePath)) {
                // Physical file doesn't exist, remove from database
                try {
                    fileModelRepository.deleteById(fileModel.getId());
                    deletedFiles++;
                } catch (Exception e) {
                    errors++;
                }
            }
        }

        return Map.of(
            "deletedRecords", deletedFiles,
            "errors", errors,
            "message", "Cleanup completed"
        );
    }

    // Helper methods

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }

    private String generateUniqueFilename(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename != null ? 
            originalFilename.substring(0, originalFilename.lastIndexOf('.')) : "file";
        return baseName + "_" + uuid + extension;
    }
}
