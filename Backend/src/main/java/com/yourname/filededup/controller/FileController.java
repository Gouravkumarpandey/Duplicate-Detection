package com.yourname.filededup.controller;

import com.yourname.filededup.model.FileRecord;
import com.yourname.filededup.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

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
            
            // Validate file size (max 100MB)
            if (file.getSize() > 100 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size exceeds maximum limit of 100MB"));
            }
            
            // Process the uploaded file with user credentials
            FileRecord processedFile = fileService.processUploadedFileWithCredentials(file, email, password);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "File uploaded and processed successfully",
                "file", processedFile,
                "userEmail", email,
                "originalFileName", file.getOriginalFilename(),
                "fileSize", file.getSize(),
                "mimeType", file.getContentType()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Failed to process uploaded file: " + e.getMessage()
                ));
        }
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

    @GetMapping("/duplicates")
    public ResponseEntity<List<FileRecord>> getDuplicates() {
        try {
            List<FileRecord> duplicates = fileService.findDuplicates();
            return ResponseEntity.ok(duplicates);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

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
    public ResponseEntity<Map<String, Long>> getFileCountByCategory() {
        try {
            Map<String, Long> categoryCounts = fileService.getFileCountByCategory();
            return ResponseEntity.ok(categoryCounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File ID is required"));
            }
            
            fileService.deleteFile(id);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    @DeleteMapping("/duplicates")
    public ResponseEntity<Map<String, Object>> deleteDuplicates() {
        try {
            int deletedCount = fileService.deleteDuplicates();
            return ResponseEntity.ok(Map.of(
                "message", "Duplicate files deleted successfully",
                "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete duplicates: " + e.getMessage()));
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

    @GetMapping("/{id}")
    public ResponseEntity<FileRecord> getFileById(@PathVariable String id) {
        try {
            if (id == null || id.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            FileRecord file = fileService.getFileById(id);
            if (file != null) {
                return ResponseEntity.ok(file);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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

    @PostMapping("/batch-delete")
    public ResponseEntity<Map<String, Object>> batchDeleteFiles(@RequestBody List<String> fileIds) {
        try {
            if (fileIds == null || fileIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File IDs are required"));
            }
            
            int deletedCount = fileService.batchDeleteFiles(fileIds);
            return ResponseEntity.ok(Map.of(
                "message", "Files deleted successfully",
                "deletedCount", deletedCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete files: " + e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveFiles(@RequestBody List<Map<String, Object>> fileData) {
        try {
            if (fileData == null || fileData.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "File data is required"));
            }
            
            int savedCount = fileService.saveFiles(fileData);
            return ResponseEntity.ok(Map.of(
                "message", "Files saved successfully",
                "savedCount", savedCount,
                "savedFiles", fileData.stream().map(f -> f.get("path")).toList(),
                "failedFiles", List.of(),
                "totalRequested", fileData.size(),
                "successCount", savedCount,
                "failureCount", 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to save files: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<FileRecord>> searchFiles(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long minSize,
            @RequestParam(required = false) Long maxSize) {
        try {
            List<FileRecord> files = fileService.searchFiles(fileName, category, minSize, maxSize);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}