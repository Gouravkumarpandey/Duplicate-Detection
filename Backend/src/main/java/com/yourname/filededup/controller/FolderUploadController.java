package com.yourname.filededup.controller;

import com.yourname.filededup.dto.FolderUploadResponse;
import com.yourname.filededup.dto.FileUploadResult;
import com.yourname.filededup.service.FolderUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for handling folder uploads with multiple files
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5175", "http://localhost:5176"})
public class FolderUploadController {

    @Autowired
    private FolderUploadService folderUploadService;

    /**
     * Upload multiple files from a folder
     * POST /api/upload-folder
     * 
     * Accepts multiple files and processes each one:
     * - Validates file types (.txt, .pdf, .docx)
     * - Computes SHA-256 hash
     * - Checks for duplicates in MongoDB
     * - Saves to uploads/ directory
     * - Stores metadata in MongoDB Atlas
     */
    @PostMapping("/upload-folder")
    public ResponseEntity<FolderUploadResponse> uploadFolder(
            @RequestParam("files") MultipartFile[] files) {
        
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest()
                .body(FolderUploadResponse.error("No files provided for upload"));
        }

        try {
            FolderUploadResponse response = folderUploadService.uploadMultipleFiles(files);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(FolderUploadResponse.error("Failed to upload folder: " + e.getMessage()));
        }
    }

    /**
     * Get upload statistics for the last folder upload
     * GET /api/upload-folder/stats
     */
    @GetMapping("/upload-folder/stats")
    public ResponseEntity<Object> getUploadStats() {
        try {
            Object stats = folderUploadService.getUploadStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Failed to get upload statistics: " + e.getMessage());
        }
    }
}
