package com.yourname.filededup.controller;

import com.yourname.filededup.model.FileModel;
import com.yourname.filededup.service.FileUploadService;
import com.yourname.filededup.dto.FileUploadResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for file upload operations with duplicate detection
 */
@RestController
@RequestMapping("/api/uploads")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5175"})
public class FileUploadController {

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * Upload a file with duplicate detection
     * POST /api/uploads/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty", "status", 400));
            }

            // Check file type
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid filename", "status", 400));
            }

            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            if (!isValidFileType(fileExtension)) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "Invalid file type. Only .txt, .pdf, and .docx files are allowed",
                        "status", 400,
                        "allowedTypes", List.of(".txt", ".pdf", ".docx")
                    ));
            }

            // Process file upload
            FileUploadResponse response = fileUploadService.uploadFile(file);
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage(), "status", 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Internal server error: " + e.getMessage(),
                    "status", 500
                ));
        }
    }

    /**
     * Get all uploaded files
     * GET /api/files/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<FileModel>> getAllFiles() {
        try {
            List<FileModel> files = fileUploadService.getAllFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all duplicate files
     * GET /api/files/duplicates
     */
    @GetMapping("/duplicates")
    public ResponseEntity<List<FileModel>> getDuplicateFiles() {
        try {
            List<FileModel> duplicates = fileUploadService.getDuplicateFiles();
            return ResponseEntity.ok(duplicates);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all unique files
     * GET /api/files/unique
     */
    @GetMapping("/unique")
    public ResponseEntity<List<FileModel>> getUniqueFiles() {
        try {
            List<FileModel> unique = fileUploadService.getUniqueFiles();
            return ResponseEntity.ok(unique);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if a file exists by hash
     * GET /api/files/exists/{hash}
     */
    @GetMapping("/exists/{hash}")
    public ResponseEntity<Map<String, Object>> checkFileExists(@PathVariable String hash) {
        try {
            boolean exists = fileUploadService.fileExistsByHash(hash);
            return ResponseEntity.ok(Map.of(
                "exists", exists,
                "hash", hash
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file statistics
     * GET /api/files/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getFileStatistics() {
        try {
            Map<String, Object> stats = fileUploadService.getFileStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a file by ID
     * DELETE /api/files/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String id) {
        try {
            boolean deleted = fileUploadService.deleteFile(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    /**
     * Get upload configuration
     * GET /api/files/config
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getUploadConfig() {
        return ResponseEntity.ok(Map.of(
            "allowedExtensions", List.of(".txt", ".pdf", ".docx"),
            "allowedMimeTypes", List.of(
                "text/plain",
                "application/pdf", 
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ),
            "maxFileSize", "100MB",
            "uploadDirectory", "uploads/"
        ));
    }

    // Helper methods
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    private boolean isValidFileType(String extension) {
        return extension.equals(".txt") || 
               extension.equals(".pdf") || 
               extension.equals(".docx");
    }
}
