package com.yourname.filededup.controller;

import com.yourname.filededup.model.FileRecord;
import com.yourname.filededup.service.FileService;
import com.yourname.filededup.service.FileContentService;
import com.yourname.filededup.service.FileStorageService;
import com.yourname.filededup.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FileContentService fileContentService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileRepository fileRepository;

    // ============ CORE FILE OPERATIONS ============

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate input parameters
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required"));
            }
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is required"));
            }
            
            // Enhanced File Type Validation - Only .txt, .pdf, .docx files
            String fileType = file.getContentType();
            String fileName = file.getOriginalFilename();
            
            if (!isValidFileType(fileType, fileName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "error", "Unsupported file type. Only .txt, .pdf, and .docx files are allowed.",
                        "receivedType", fileType != null ? fileType : "unknown",
                        "fileName", fileName != null ? fileName : "unknown",
                        "allowedTypes", List.of("text/plain", "application/pdf", 
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                        "allowedExtensions", List.of(".txt", ".pdf", ".docx")
                    ));
            }
            
            // Validate file size (max 100MB)
            if (file.getSize() > 100 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size exceeds maximum limit of 100MB"));
            }
            
            // Process the uploaded file with user credentials
            FileRecord processedFile = fileService.processUploadedFileWithCredentials(file, email, password);
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("success", true);
            responseMap.put("message", "File uploaded and processed successfully");
            responseMap.put("file", processedFile);
            responseMap.put("userEmail", email);
            responseMap.put("originalFileName", file.getOriginalFilename());
            responseMap.put("fileSize", file.getSize());
            responseMap.put("formattedSize", processedFile.getFormattedFileSize());
            responseMap.put("mimeType", file.getContentType());
            responseMap.put("fileType", "validated");
            responseMap.put("isDuplicate", processedFile.isDuplicate());
            responseMap.put("category", processedFile.getCategory());
            
            return ResponseEntity.ok(responseMap);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Failed to process uploaded file: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            // Validate input parameters
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password is required"));
            }
            
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "At least one file is required"));
            }
            
            if (files.length > 10) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Maximum 10 files allowed per upload"));
            }
            
            // Validate file types for all uploaded files
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Empty files are not allowed"));
                }
                
                String fileType = file.getContentType();
                String fileName = file.getOriginalFilename();
                
                if (!isValidFileType(fileType, fileName)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "error", "Unsupported file type found: " + (fileName != null ? fileName : "unknown"),
                            "invalidFile", fileName != null ? fileName : "unknown",
                            "receivedType", fileType != null ? fileType : "unknown",
                            "allowedTypes", List.of("text/plain", "application/pdf", 
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                            "allowedExtensions", List.of(".txt", ".pdf", ".docx")
                        ));
                }
                
                // Validate file size (max 100MB per file)
                if (file.getSize() > 100 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size exceeds maximum limit of 100MB: " + 
                            (fileName != null ? fileName : "unknown")));
                }
            }
            
            Map<String, Object> result = fileService.processMultipleFiles(files, email, password);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Failed to process uploaded files: " + e.getMessage()
                ));
        }
    }

    // ============ ENHANCED FILE UPLOAD WITH CONTENT PROCESSING ============

    @PostMapping("/upload-enhanced")
    public ResponseEntity<Map<String, Object>> uploadFileEnhanced(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // 1. Basic file validation
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is required"));
            }

            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            
            // 2. Validate file type (only .txt, .pdf, .docx)
            if (!isValidFileType(contentType, fileName)) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Only .txt, .pdf, and .docx files are allowed",
                        "receivedType", contentType != null ? contentType : "unknown",
                        "fileName", fileName != null ? fileName : "unknown",
                        "allowedTypes", List.of("text/plain", "application/pdf", 
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    ));
            }

            // 3. Validate file size (max 100MB)
            if (file.getSize() > 100 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size exceeds maximum limit of 100MB"));
            }

            // 4. Get file extension
            String fileExtension = fileContentService.getFileExtension(fileName);
            
            // 5. Extract text content based on file type
            String textContent;
            try {
                textContent = fileContentService.extractTextContent(file, fileExtension);
            } catch (Exception e) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to extract content from file: " + e.getMessage()));
            }

            // 6. Calculate SHA-256 hash of the content
            String contentHash = fileContentService.calculateContentHash(textContent);
            
            // 7. Check MongoDB for existing hash (duplicate detection)
            List<FileRecord> existingFiles = fileRepository.findByFileHash(contentHash);
            if (!existingFiles.isEmpty()) {
                FileRecord existingFile = existingFiles.get(0);
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "isDuplicate", true,
                    "message", "Duplicate file detected",
                    "existingFile", Map.of(
                        "id", existingFile.getId(),
                        "fileName", existingFile.getFileName(),
                        "uploadDate", existingFile.getUploadedDate(),
                        "filePath", existingFile.getFilePath(),
                        "hash", existingFile.getFileHash()
                    ),
                    "contentHash", contentHash
                ));
            }

            // 8. Generate unique filename and save to /uploads folder
            String uniqueFileName = fileContentService.generateUniqueFileName(fileName);
            
            // Create uploads directory if it doesn't exist
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            
            // Save file to uploads directory
            Path targetPath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            String filePath = targetPath.toString();

            // 9. Create FileRecord with metadata and save to MongoDB
            FileRecord fileRecord = new FileRecord();
            fileRecord.setFileName(fileName);
            fileRecord.setFilePath(filePath);
            fileRecord.setFileHash(contentHash);
            fileRecord.setFileSize(file.getSize());
            fileRecord.setFileExtension(fileExtension);
            fileRecord.setMimeType(contentType);
            fileRecord.setUploadedDate(LocalDateTime.now());
            fileRecord.setCreatedDate(LocalDateTime.now());
            fileRecord.setStoredFileName(uniqueFileName);
            fileRecord.setChecksum(contentHash);
            fileRecord.setChecksumAlgorithm("SHA-256");
            fileRecord.setVerified(true);
            fileRecord.setLastVerifiedDate(LocalDateTime.now());
            fileRecord.setContentPreview(textContent.length() > 500 ? 
                textContent.substring(0, 500) + "..." : textContent);
            fileRecord.setHasPreview(true);
            fileRecord.setDuplicate(false);

            // Save to MongoDB
            FileRecord savedFile = fileRepository.save(fileRecord);

            // 10. Return success response with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("isDuplicate", false);
            response.put("message", "File uploaded and processed successfully");
            response.put("fileId", savedFile.getId());
            response.put("fileName", fileName);
            response.put("fileSize", file.getSize());
            response.put("fileType", fileExtension);
            response.put("uploadDate", savedFile.getUploadedDate());
            response.put("filePath", filePath);
            response.put("contentHash", contentHash);
            response.put("contentPreview", fileRecord.getContentPreview());
            response.put("textContentLength", textContent.length());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to process file upload: " + e.getMessage()
                ));
        }
    }

    // ============ TEST ENDPOINTS FOR ENHANCED UPLOAD ============

    @PostMapping("/test-content-extraction")
    public ResponseEntity<Map<String, Object>> testContentExtraction(
            @RequestParam("file") MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String fileExtension = fileContentService.getFileExtension(fileName);
            
            if (!fileContentService.isValidFileType(fileName, file.getContentType())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid file type for testing"));
            }
            
            String content = fileContentService.extractTextContent(file, fileExtension);
            String hash = fileContentService.calculateContentHash(content);
            
            return ResponseEntity.ok(Map.of(
                "fileName", fileName,
                "fileExtension", fileExtension,
                "contentLength", content.length(),
                "contentPreview", content.length() > 200 ? content.substring(0, 200) + "..." : content,
                "contentHash", hash,
                "mimeType", file.getContentType(),
                "fileSize", file.getSize()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Test failed: " + e.getMessage()));
        }
    }

    @GetMapping("/storage-info-enhanced")
    public ResponseEntity<Map<String, Object>> getEnhancedStorageInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("uploadDirectory", "uploads/");
        info.put("maxFileSize", "100MB");
        info.put("allowedExtensions", List.of(".txt", ".pdf", ".docx"));
        info.put("allowedMimeTypes", List.of(
            "text/plain", 
            "application/pdf", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ));
        info.put("hashAlgorithm", "SHA-256");
        info.put("contentExtraction", Map.of(
            ".txt", "BufferedReader with UTF-8",
            ".pdf", "Apache PDFBox PDFTextStripper",
            ".docx", "Apache POI XWPFWordExtractor"
        ));
        info.put("duplicateDetection", "Content-based SHA-256 hash comparison");
        
        return ResponseEntity.ok(info);
    }

    // Enhanced helper method for strict file type validation
    private boolean isValidFileType(String contentType, String fileName) {
        // Strict validation - only allow specific file types
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

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanDirectory(@RequestParam String directoryPath) {
        try {
            if (directoryPath == null || directoryPath.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Directory path is required"));
            }
            
            Map<String, Object> result = fileService.scanDirectory(directoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to scan directory: " + e.getMessage()));
        }
    }

    // ============ FILE RETRIEVAL OPERATIONS ============

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllFilesWithMetadata(
            @RequestParam(required = false) Boolean duplicate,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder) {
        try {
            Map<String, Object> result = fileService.getFilesWithFilters(
                duplicate, uploadedBy, category, page, size, sortBy, sortOrder);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to retrieve files: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<FileRecord>> getAllFiles() {
        try {
            List<FileRecord> files = fileService.getAllFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileRecord> getFileById(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            FileRecord file = fileService.getFileById(id);
            if (file != null) {
                // Update access tracking
                file.updateLastAccess();
                fileService.updateFile(file);
                return ResponseEntity.ok(file);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchFiles(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String tags,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> result = fileService.searchFilesAdvanced(
                fileName, category, minSize, maxSize, uploadedBy, tags, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to search files: " + e.getMessage()));
        }
    }

    // ============ DUPLICATE MANAGEMENT ============

    @GetMapping("/duplicates")
    public ResponseEntity<Map<String, Object>> getDuplicates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Map<String, Object> result = fileService.getDuplicatesWithPagination(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get duplicates: " + e.getMessage()));
        }
    }

    @GetMapping("/duplicate-groups")
    public ResponseEntity<Map<String, Object>> getDuplicateGroups() {
        try {
            Map<String, Object> duplicateGroups = fileService.getDuplicateGroups();
            return ResponseEntity.ok(duplicateGroups);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get duplicate groups: " + e.getMessage()));
        }
    }

    @PostMapping("/resolve-duplicates")
    public ResponseEntity<Map<String, Object>> resolveDuplicates(
            @RequestBody Map<String, Object> resolutionData) {
        try {
            Map<String, Object> result = fileService.resolveDuplicates(resolutionData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to resolve duplicates: " + e.getMessage()));
        }
    }

    // ============ CATEGORY MANAGEMENT ============

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FileRecord>> getFilesByCategory(@PathVariable String category) {
        try {
            if (category == null || category.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<FileRecord> files = fileService.getFilesByCategory(category);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getFileCountByCategory() {
        try {
            Map<String, Object> categoryData = fileService.getCategoryStatistics();
            return ResponseEntity.ok(categoryData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get category statistics: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/category")
    public ResponseEntity<Map<String, String>> updateFileCategory(
            @PathVariable String id,
            @RequestParam String category) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ID is required"));
            }
            
            if (category == null || category.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Category is required"));
            }
            
            fileService.updateFileCategory(id, category);
            return ResponseEntity.ok(Map.of("message", "File category updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update file category: " + e.getMessage()));
        }
    }

    @PostMapping("/bulk-categorize")
    public ResponseEntity<Map<String, Object>> bulkCategorizeFiles(@RequestBody Map<String, String> categoryUpdates) {
        try {
            if (categoryUpdates == null || categoryUpdates.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Category updates are required"));
            }
            
            int updatedCount = fileService.bulkUpdateCategories(categoryUpdates);
            return ResponseEntity.ok(Map.of(
                "message", "Categories updated successfully",
                "updatedCount", updatedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update categories: " + e.getMessage()));
        }
    }

    // ============ USER MANAGEMENT ============

    @GetMapping("/user/{email}")
    public ResponseEntity<Map<String, Object>> getFilesByUser(
            @PathVariable String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            Map<String, Object> result = fileService.getFilesByUserWithPagination(email, page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get user files: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{email}/stats")
    public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            Map<String, Object> userStats = fileService.getUserStatistics(email);
            return ResponseEntity.ok(userStats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get user statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            Map<String, Object> usersData = fileService.getAllUsersWithStats();
            return ResponseEntity.ok(usersData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get users data: " + e.getMessage()));
        }
    }

    // ============ DELETE OPERATIONS ============

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ID is required"));
            }
            
            boolean deleted = fileService.deleteFile(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "File not found");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/duplicates")
    public ResponseEntity<Map<String, Object>> deleteDuplicates(
            @RequestParam(defaultValue = "false") boolean keepOldest) {
        try {
            Map<String, Object> result = fileService.deleteDuplicatesAdvanced(keepOldest);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete duplicates: " + e.getMessage()));
        }
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDeleteFiles(@RequestBody Map<String, Object> deleteRequest) {
        try {
            @SuppressWarnings("unchecked")
            List<String> fileIds = (List<String>) deleteRequest.get("fileIds");
            Boolean deletePhysical = (Boolean) deleteRequest.getOrDefault("deletePhysical", false);
            
            if (fileIds == null || fileIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File IDs are required"));
            }
            
            Map<String, Object> result = fileService.batchDeleteFilesAdvanced(fileIds, deletePhysical);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete files: " + e.getMessage()));
        }
    }

    @DeleteMapping("/user/{email}")
    public ResponseEntity<Map<String, Object>> deleteUserFiles(@PathVariable String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required"));
            }
            
            Map<String, Object> result = fileService.deleteFilesByUser(email);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete user files: " + e.getMessage()));
        }
    }

    // ============ STATISTICS & ANALYTICS ============

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFileStats() {
        try {
            Map<String, Object> stats = fileService.getFileStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get file statistics: " + e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            Map<String, Object> dashboardData = fileService.getDashboardStatistics();
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get dashboard data: " + e.getMessage()));
        }
    }

    @GetMapping("/recent")
    public ResponseEntity<List<FileRecord>> getRecentFiles(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<FileRecord> recentFiles = fileService.getRecentFiles(hours, limit);
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/size-analysis")
    public ResponseEntity<Map<String, Object>> getFileSizeAnalysis() {
        try {
            Map<String, Object> sizeAnalysis = fileService.getFileSizeAnalysis();
            return ResponseEntity.ok(sizeAnalysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get file size analysis: " + e.getMessage()));
        }
    }

    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getUploadTrends(
            @RequestParam(defaultValue = "30") int days) {
        try {
            Map<String, Object> trends = fileService.getUploadTrends(days);
            return ResponseEntity.ok(trends);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get upload trends: " + e.getMessage()));
        }
    }

    // ============ FILE INTEGRITY & VERIFICATION ============

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyFileIntegrity(@RequestParam String fileId) {
        try {
            if (fileId == null || fileId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ID is required"));
            }
            
            Map<String, Object> verificationResult = fileService.verifyFileIntegrity(fileId);
            return ResponseEntity.ok(verificationResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to verify file integrity: " + e.getMessage()));
        }
    }

    @PostMapping("/verify-all")
    public ResponseEntity<Map<String, Object>> verifyAllFiles() {
        try {
            Map<String, Object> verificationResult = fileService.verifyAllFiles();
            return ResponseEntity.ok(verificationResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to verify files: " + e.getMessage()));
        }
    }

    @PostMapping("/rescan-duplicates")
    public ResponseEntity<Map<String, Object>> rescanForDuplicates() {
        try {
            Map<String, Object> rescanResult = fileService.rescanForDuplicates();
            return ResponseEntity.ok(rescanResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to rescan for duplicates: " + e.getMessage()));
        }
    }

    // ============ MAINTENANCE & CLEANUP ============

    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOrphanedFiles() {
        try {
            Map<String, Object> cleanupResult = fileService.cleanupOrphanedFiles();
            return ResponseEntity.ok(cleanupResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to cleanup orphaned files: " + e.getMessage()));
        }
    }

    @PostMapping("/optimize-storage")
    public ResponseEntity<Map<String, Object>> optimizeStorage() {
        try {
            Map<String, Object> optimizationResult = fileService.optimizeStorage();
            return ResponseEntity.ok(optimizationResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to optimize storage: " + e.getMessage()));
        }
    }

    // ============ EXPORT & IMPORT ============

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportFileList(
            @RequestParam(required = false, defaultValue = "json") String format,
            @RequestParam(required = false) Boolean duplicatesOnly,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String uploadedBy) {
        try {
            if (!format.equals("json") && !format.equals("csv") && !format.equals("xlsx")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Unsupported export format. Use 'json', 'csv', or 'xlsx'"));
            }
            
            Map<String, Object> exportResult = fileService.exportFileList(format, duplicatesOnly, category, uploadedBy);
            return ResponseEntity.ok(exportResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to export file list: " + e.getMessage()));
        }
    }

    @PostMapping("/import-metadata")
    public ResponseEntity<Map<String, Object>> importMetadata(@RequestParam("file") MultipartFile metadataFile) {
        try {
            if (metadataFile == null || metadataFile.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Metadata file is required"));
            }
            
            Map<String, Object> importResult = fileService.importMetadata(metadataFile);
            return ResponseEntity.ok(importResult);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to import metadata: " + e.getMessage()));
        }
    }

    // ============ FILE TAGGING & METADATA ============

    @PutMapping("/{id}/tags")
    public ResponseEntity<Map<String, Object>> updateFileTags(
            @PathVariable String id,
            @RequestBody Map<String, String> tagData) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ID is required"));
            }
            
            String tags = tagData.get("tags");
            if (tags == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tags are required"));
            }
            
            FileRecord updatedFile = fileService.updateFileTags(id, tags);
            return ResponseEntity.ok(Map.of(
                "message", "File tags updated successfully",
                "file", updatedFile
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update file tags: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/description")
    public ResponseEntity<Map<String, Object>> updateFileDescription(
            @PathVariable String id,
            @RequestBody Map<String, String> descData) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ID is required"));
            }
            
            String description = descData.get("description");
            FileRecord updatedFile = fileService.updateFileDescription(id, description);
            return ResponseEntity.ok(Map.of(
                "message", "File description updated successfully",
                "file", updatedFile
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update file description: " + e.getMessage()));
        }
    }

    // ============ HEALTH CHECK ============

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = fileService.getSystemHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "unhealthy", "error", e.getMessage()));
        }
    }

    // ============ FILE VALIDATION AND STORAGE INFO ENDPOINTS ============

    @PostMapping("/validate-type")
    public ResponseEntity<Map<String, Object>> validateFileType(@RequestParam("file") MultipartFile file) {
        try {
            String fileType = file.getContentType();
            String fileName = file.getOriginalFilename();
            
            boolean isValid = isValidFileType(fileType, fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);
            response.put("fileName", fileName);
            response.put("contentType", fileType);
            response.put("fileSize", file.getSize());
            
            if (isValid) {
                response.put("message", "File type is supported");
                response.put("allowedTypes", List.of("text/plain", "application/pdf", 
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            } else {
                response.put("error", "Unsupported file type. Only .txt, .pdf, and .docx files are allowed.");
                response.put("allowedExtensions", List.of(".txt", ".pdf", ".docx"));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to validate file: " + e.getMessage()));
        }
    }

    @GetMapping("/storage-info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        try {
            Map<String, Object> storageInfo = new HashMap<>();
            storageInfo.put("maxFileSize", "100MB");
            storageInfo.put("maxFileSizeBytes", 100 * 1024 * 1024);
            storageInfo.put("allowedTypes", List.of("text/plain", "application/pdf", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            storageInfo.put("allowedExtensions", List.of(".txt", ".pdf", ".docx"));
            storageInfo.put("storageOptions", List.of("LOCAL", "GRIDFS", "S3", "GOOGLE_CLOUD"));
            storageInfo.put("currentStorageType", "LOCAL"); // This could be read from configuration
            
            return ResponseEntity.ok(storageInfo);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to get storage info: " + e.getMessage()));
        }
    }

    @GetMapping("/supported-types")
    public ResponseEntity<Map<String, Object>> getSupportedFileTypes() {
        Map<String, Object> supportedTypes = new HashMap<>();
        supportedTypes.put("mimeTypes", List.of(
            "text/plain",
            "application/pdf", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        ));
        supportedTypes.put("extensions", List.of(".txt", ".pdf", ".docx"));
        supportedTypes.put("descriptions", Map.of(
            "text/plain", "Plain text files (.txt)",
            "application/pdf", "Portable Document Format (.pdf)",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Microsoft Word documents (.docx)"
        ));
        
        return ResponseEntity.ok(supportedTypes);
    }
}