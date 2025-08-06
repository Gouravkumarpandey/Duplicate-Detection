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

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> scanDirectory(@RequestParam String directoryPath) {
        try {
            Map<String, Object> result = fileService.scanDirectory(directoryPath);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to scan directory: " + e.getMessage()));
        }
    }

    @GetMapping("/duplicates")
    public ResponseEntity<List<FileRecord>> getDuplicates() {
        List<FileRecord> duplicates = fileService.findDuplicates();
        return ResponseEntity.ok(duplicates);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<FileRecord>> getFilesByCategory(@PathVariable String category) {
        List<FileRecord> files = fileService.getFilesByCategory(category);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable String id) {
        try {
            fileService.deleteFile(id);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<FileRecord>> getAllFiles() {
        List<FileRecord> files = fileService.getAllFiles();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            fileService.processUploadedFile(file);
            return ResponseEntity.ok(Map.of("message", "File uploaded and processed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to process uploaded file: " + e.getMessage()));
        }
    }
}
