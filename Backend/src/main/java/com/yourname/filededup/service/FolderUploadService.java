package com.yourname.filededup.service;

import com.yourname.filededup.dto.FolderUploadResponse;
import com.yourname.filededup.dto.FileUploadResult;
import com.yourname.filededup.model.FileModel;
import com.yourname.filededup.repository.FileModelRepository;
import com.yourname.filededup.util.FileHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for handling folder uploads with multiple files
 */
@Service
public class FolderUploadService {

    @Autowired
    private FileModelRepository fileModelRepository;

    @Autowired
    private FileValidationService fileValidationService;

    @Value("${app.file.upload-dir:uploads/}")
    private String uploadDirectory;

    @Value("${app.file.max-size:104857600}") // 100MB default
    private long maxFileSize;

    // Allowed file types as per requirements
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".pdf", ".docx");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "text/plain",
        "application/pdf", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /**
     * Upload multiple files from a folder
     */
    public FolderUploadResponse uploadMultipleFiles(MultipartFile[] files) {
        List<FileUploadResult> uploadResults = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Ensure upload directory exists
        createUploadDirectoryIfNotExists();

        // Process each file
        for (MultipartFile file : files) {
            try {
                FileUploadResult result = processIndividualFile(file);
                uploadResults.add(result);

                if (result.isSuccess()) {
                    successCount.incrementAndGet();
                    if (result.isDuplicate()) {
                        duplicateCount.incrementAndGet();
                    }
                } else {
                    errorCount.incrementAndGet();
                }
            } catch (Exception e) {
                FileUploadResult errorResult = FileUploadResult.error(
                    file.getOriginalFilename(), 
                    "Processing failed: " + e.getMessage()
                );
                uploadResults.add(errorResult);
                errorCount.incrementAndGet();
            }
        }

        // Create response
        String message = String.format(
            "Folder upload completed: %d successful, %d duplicates, %d errors", 
            successCount.get(), duplicateCount.get(), errorCount.get()
        );

        FolderUploadResponse response = FolderUploadResponse.success(message, uploadResults);
        response.setStatistics(generateUploadStatistics(uploadResults));

        return response;
    }

    /**
     * Process an individual file
     */
    private FileUploadResult processIndividualFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();

        // Step 1: Validate file type
        if (!isValidFileType(file)) {
            return FileUploadResult.error(originalFilename, 
                "Invalid file type. Only .txt, .pdf, and .docx files are allowed.");
        }

        // Step 2: Validate file size
        if (file.getSize() > maxFileSize) {
            return FileUploadResult.error(originalFilename, 
                "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Step 3: Compute SHA-256 hash
        String hash = FileHashUtil.calculateSHA256(file.getInputStream());

        // Step 4: Check for duplicates in MongoDB
        Optional<FileModel> existingFile = fileModelRepository.findByHash(hash);
        boolean isDuplicate = existingFile.isPresent();

        // Step 5: Generate unique filename
        String uniqueFilename = generateUniqueFilename(originalFilename);

        // Step 6: Save file to uploads/ directory
        Path uploadPath = Paths.get(uploadDirectory);
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Step 7: Create FileModel and save to MongoDB Atlas
        FileModel fileModel = new FileModel();
        fileModel.setFilename(uniqueFilename);
        fileModel.setOriginalName(originalFilename);
        fileModel.setFileType(getFileExtension(originalFilename));
        fileModel.setHash(hash);
        fileModel.setUploadDate(LocalDateTime.now());
        fileModel.setFileSize(file.getSize());
        fileModel.setMimeType(file.getContentType());
        fileModel.setFilePath(filePath.toString());
        fileModel.setIsDuplicate(isDuplicate);

        // Save to MongoDB
        FileModel savedFile = fileModelRepository.save(fileModel);

        return FileUploadResult.success(originalFilename, savedFile, isDuplicate);
    }

    /**
     * Validate file type (.txt, .pdf, .docx only)
     */
    private boolean isValidFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String mimeType = file.getContentType();

        if (filename == null) return false;

        // Check file extension
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return false;
        }

        // Check MIME type
        if (mimeType != null && !ALLOWED_MIME_TYPES.contains(mimeType)) {
            return false;
        }

        return true;
    }

    /**
     * Create upload directory if it doesn't exist
     */
    private void createUploadDirectoryIfNotExists() {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory: " + e.getMessage(), e);
        }
    }

    /**
     * Generate unique filename to avoid conflicts
     */
    private String generateUniqueFilename(String originalFilename) {
        if (originalFilename == null) return "file_" + UUID.randomUUID().toString();

        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
        return baseName + "_" + UUID.randomUUID().toString() + extension;
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex == -1 ? "" : filename.substring(lastDotIndex);
    }

    /**
     * Generate upload statistics
     */
    private Map<String, Object> generateUploadStatistics(List<FileUploadResult> results) {
        Map<String, Object> stats = new HashMap<>();
        
        long totalFiles = results.size();
        long successfulUploads = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
        long failedUploads = results.stream().mapToLong(r -> !r.isSuccess() ? 1 : 0).sum();
        long duplicatesFound = results.stream().mapToLong(r -> r.isDuplicate() ? 1 : 0).sum();
        long totalSizeBytes = results.stream()
            .filter(FileUploadResult::isSuccess)
            .mapToLong(FileUploadResult::getFileSize)
            .sum();

        // File type distribution
        Map<String, Long> fileTypeDistribution = new HashMap<>();
        results.stream()
            .filter(FileUploadResult::isSuccess)
            .forEach(r -> {
                String fileType = r.getFileType();
                fileTypeDistribution.put(fileType, fileTypeDistribution.getOrDefault(fileType, 0L) + 1);
            });

        stats.put("totalFiles", totalFiles);
        stats.put("successfulUploads", successfulUploads);
        stats.put("failedUploads", failedUploads);
        stats.put("duplicatesFound", duplicatesFound);
        stats.put("totalSizeBytes", totalSizeBytes);
        stats.put("totalSizeMB", totalSizeBytes / (1024.0 * 1024.0));
        stats.put("fileTypeDistribution", fileTypeDistribution);
        stats.put("successRate", totalFiles > 0 ? (successfulUploads * 100.0 / totalFiles) : 0.0);
        stats.put("duplicateRate", successfulUploads > 0 ? (duplicatesFound * 100.0 / successfulUploads) : 0.0);

        return stats;
    }

    /**
     * Get upload statistics
     */
    public Map<String, Object> getUploadStatistics() {
        List<FileModel> allFiles = fileModelRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        long totalFiles = allFiles.size();
        long duplicateCount = fileModelRepository.countByIsDuplicateTrue();
        long uniqueCount = fileModelRepository.countByIsDuplicateFalse();
        long totalSizeBytes = allFiles.stream().mapToLong(FileModel::getFileSize).sum();

        stats.put("totalFilesInDatabase", totalFiles);
        stats.put("duplicateFiles", duplicateCount);
        stats.put("uniqueFiles", uniqueCount);
        stats.put("totalSizeBytes", totalSizeBytes);
        stats.put("totalSizeMB", totalSizeBytes / (1024.0 * 1024.0));
        stats.put("duplicatePercentage", totalFiles > 0 ? (duplicateCount * 100.0 / totalFiles) : 0.0);

        return stats;
    }
}
