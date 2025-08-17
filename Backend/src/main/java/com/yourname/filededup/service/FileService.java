package com.yourname.filededup.service;

import com.yourname.filededup.model.FileRecord;
import com.yourname.filededup.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private RuleEngineService ruleEngineService;

    @Autowired
    private LoggingService loggingService;

    public FileRecord processUploadedFileWithCredentials(MultipartFile file, String email, String password) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        // Log the upload attempt with user credentials
        loggingService.logInfo("File upload initiated", "UPLOAD", 
            "User: " + email + ", File: " + fileName + ", Size: " + file.getSize() + " bytes");

        // Create temporary file
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        file.transferTo(tempFile.toFile());

        try {
            FileRecord fileRecord = createFileRecord(tempFile);
            fileRecord.setFileName(fileName);
            fileRecord.setScannedDate(LocalDateTime.now());
            
            // Categorize the file
            String category = ruleEngineService.categorizeFile(fileRecord);
            fileRecord.setCategory(category);
            
            // Check for duplicates
            List<FileRecord> duplicates = fileRepository.findByFileHash(fileRecord.getFileHash());
            if (!duplicates.isEmpty()) {
                fileRecord.setDuplicate(true);
                loggingService.logWarn("Duplicate file uploaded", "UPLOAD", 
                    "User: " + email + ", File: " + fileName + ", Hash: " + fileRecord.getFileHash());
            }
            
            FileRecord savedFile = fileRepository.save(fileRecord);
            
            loggingService.logInfo("File uploaded and processed successfully", "UPLOAD", 
                "User: " + email + ", File: " + fileName + ", Category: " + category + ", ID: " + savedFile.getId());
            
            return savedFile;
            
        } finally {
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
        }
    }

    public Map<String, Object> scanDirectory(String directoryPath) throws IOException {
        loggingService.logInfo("Starting directory scan", "SCAN", "Directory: " + directoryPath);
        
        Path path = Paths.get(directoryPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }

        List<FileRecord> scannedFiles = new ArrayList<>();
        int totalFiles = 0;
        int duplicateCount = 0;

        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> filePaths = paths
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());

            totalFiles = filePaths.size();

            for (Path filePath : filePaths) {
                try {
                    FileRecord fileRecord = createFileRecord(filePath);
                    
                    // Check if file already exists in database
                    if (!fileRepository.existsByFilePath(fileRecord.getFilePath())) {
                        // Categorize the file
                        String category = ruleEngineService.categorizeFile(fileRecord);
                        fileRecord.setCategory(category);
                        
                        // Check for duplicates
                        List<FileRecord> duplicates = fileRepository.findByFileHash(fileRecord.getFileHash());
                        if (!duplicates.isEmpty()) {
                            fileRecord.setDuplicate(true);
                            duplicateCount++;
                        }
                        
                        scannedFiles.add(fileRepository.save(fileRecord));
                    }
                } catch (Exception e) {
                    loggingService.logError("Failed to process file", "SCAN", 
                        "File: " + filePath + ", Error: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", totalFiles);
        result.put("scannedFiles", scannedFiles.size());
        result.put("duplicateCount", duplicateCount);
        result.put("files", scannedFiles);

        loggingService.logInfo("Directory scan completed", "SCAN", 
            "Processed: " + scannedFiles.size() + " files, Duplicates: " + duplicateCount);

        return result;
    }

    public List<FileRecord> findDuplicates() {
        return fileRepository.findByIsDuplicate(true);
    }

    public List<FileRecord> getFilesByCategory(String category) {
        return fileRepository.findByCategory(category);
    }

    public Map<String, Long> getFileCountByCategory() {
        List<FileRecord> allFiles = fileRepository.findAll();
        return allFiles.stream()
                .collect(Collectors.groupingBy(
                    file -> file.getCategory() != null ? file.getCategory() : "Other",
                    Collectors.counting()
                ));
    }

    public List<FileRecord> getAllFiles() {
        return fileRepository.findAll();
    }

    public FileRecord getFileById(String id) {
        Optional<FileRecord> fileRecord = fileRepository.findById(id);
        return fileRecord.orElse(null);
    }

    public void updateFileCategory(String id, String category) {
        Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
        if (fileRecordOpt.isPresent()) {
            FileRecord fileRecord = fileRecordOpt.get();
            String oldCategory = fileRecord.getCategory();
            fileRecord.setCategory(category);
            fileRepository.save(fileRecord);
            
            loggingService.logInfo("File category updated", "UPDATE", 
                "File: " + fileRecord.getFileName() + ", Old: " + oldCategory + ", New: " + category);
        } else {
            throw new IllegalArgumentException("File not found with id: " + id);
        }
    }

    public void deleteFile(String id) throws IOException {
        Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
        if (fileRecordOpt.isPresent()) {
            FileRecord fileRecord = fileRecordOpt.get();
            
            // Delete physical file if it exists
            Path filePath = Paths.get(fileRecord.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            // Delete from database
            fileRepository.deleteById(id);
            
            loggingService.logInfo("File deleted", "DELETE", 
                "File: " + fileRecord.getFileName() + " (" + fileRecord.getFilePath() + ")");
        } else {
            throw new IllegalArgumentException("File not found with id: " + id);
        }
    }

    public int deleteDuplicates() throws IOException {
        List<FileRecord> duplicates = fileRepository.findByIsDuplicate(true);
        int deletedCount = 0;
        
        for (FileRecord duplicate : duplicates) {
            try {
                deleteFile(duplicate.getId());
                deletedCount++;
            } catch (Exception e) {
                loggingService.logError("Failed to delete duplicate file", "DELETE", 
                    "File: " + duplicate.getFileName() + ", Error: " + e.getMessage());
            }
        }
        
        loggingService.logInfo("Duplicate files cleanup completed", "DELETE", 
            "Deleted: " + deletedCount + " files");
        
        return deletedCount;
    }

    public int batchDeleteFiles(List<String> fileIds) throws IOException {
        int deletedCount = 0;
        
        for (String fileId : fileIds) {
            try {
                deleteFile(fileId);
                deletedCount++;
            } catch (Exception e) {
                loggingService.logError("Failed to delete file in batch operation", "DELETE", 
                    "FileId: " + fileId + ", Error: " + e.getMessage());
            }
        }
        
        loggingService.logInfo("Batch delete completed", "DELETE", 
            "Deleted: " + deletedCount + " out of " + fileIds.size() + " files");
        
        return deletedCount;
    }

    public Map<String, Object> getFileStatistics() {
        List<FileRecord> allFiles = fileRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", allFiles.size());
        stats.put("duplicateFiles", allFiles.stream().mapToLong(f -> f.isDuplicate() ? 1 : 0).sum());
        stats.put("totalSize", allFiles.stream().mapToLong(FileRecord::getFileSize).sum());
        stats.put("averageSize", allFiles.isEmpty() ? 0 : 
            allFiles.stream().mapToLong(FileRecord::getFileSize).average().orElse(0));
        
        // Category breakdown
        Map<String, Long> categoryStats = getFileCountByCategory();
        stats.put("categoryBreakdown", categoryStats);
        
        // Size breakdown
        Map<String, Long> sizeBreakdown = new HashMap<>();
        sizeBreakdown.put("small", allFiles.stream().mapToLong(f -> f.getFileSize() < 1024 ? 1 : 0).sum());
        sizeBreakdown.put("medium", allFiles.stream().mapToLong(f -> 
            f.getFileSize() >= 1024 && f.getFileSize() < 1024 * 1024 ? 1 : 0).sum());
        sizeBreakdown.put("large", allFiles.stream().mapToLong(f -> 
            f.getFileSize() >= 1024 * 1024 && f.getFileSize() < 100 * 1024 * 1024 ? 1 : 0).sum());
        sizeBreakdown.put("xlarge", allFiles.stream().mapToLong(f -> 
            f.getFileSize() >= 100 * 1024 * 1024 ? 1 : 0).sum());
        stats.put("sizeBreakdown", sizeBreakdown);
        
        return stats;
    }

    public List<FileRecord> searchFiles(String fileName, String category, Long minSize, Long maxSize) {
        List<FileRecord> allFiles = fileRepository.findAll();
        
        return allFiles.stream()
            .filter(file -> fileName == null || fileName.isEmpty() || 
                    file.getFileName().toLowerCase().contains(fileName.toLowerCase()))
            .filter(file -> category == null || category.isEmpty() || 
                    Objects.equals(file.getCategory(), category))
            .filter(file -> minSize == null || file.getFileSize() >= minSize)
            .filter(file -> maxSize == null || file.getFileSize() <= maxSize)
            .collect(Collectors.toList());
    }

    public void processUploadedFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        // Create temporary file
        Path tempFile = Files.createTempFile("upload_", "_" + fileName);
        file.transferTo(tempFile.toFile());

        try {
            FileRecord fileRecord = createFileRecord(tempFile);
            fileRecord.setFileName(fileName);
            
            // Categorize the file
            String category = ruleEngineService.categorizeFile(fileRecord);
            fileRecord.setCategory(category);
            
            // Check for duplicates
            List<FileRecord> duplicates = fileRepository.findByFileHash(fileRecord.getFileHash());
            if (!duplicates.isEmpty()) {
                fileRecord.setDuplicate(true);
            }
            
            fileRepository.save(fileRecord);
            
            loggingService.logInfo("File uploaded and processed", "UPLOAD", 
                "File: " + fileName + ", Category: " + category);
        } finally {
            // Clean up temporary file
            Files.deleteIfExists(tempFile);
        }
    }

    private FileRecord createFileRecord(Path filePath) throws IOException {
        File file = filePath.toFile();
        
        FileRecord fileRecord = new FileRecord();
        fileRecord.setFileName(file.getName());
        fileRecord.setFilePath(file.getAbsolutePath());
        fileRecord.setFileSize(file.length());
        fileRecord.setCreatedDate(LocalDateTime.ofInstant(
            new Date(file.lastModified()).toInstant(), ZoneId.systemDefault()));
        fileRecord.setModifiedDate(LocalDateTime.ofInstant(
            new Date(file.lastModified()).toInstant(), ZoneId.systemDefault()));
        fileRecord.setScannedDate(LocalDateTime.now());
        
        // Calculate file hash
        String hash = calculateFileHash(filePath);
        fileRecord.setFileHash(hash);
        
        // Set MIME type
        String mimeType = Files.probeContentType(filePath);
        fileRecord.setMimeType(mimeType);
        
        // Set file extension
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            fileRecord.setFileExtension(fileName.substring(lastDotIndex + 1).toLowerCase());
        }
        
        return fileRecord;
    }

    public int saveFiles(List<Map<String, Object>> fileDataList) {
        int savedCount = 0;
        for (Map<String, Object> fileData : fileDataList) {
            try {
                FileRecord fileRecord = new FileRecord();
                fileRecord.setFilePath(fileData.get("path").toString());
                fileRecord.setFileName(fileData.get("name").toString());
                fileRecord.setFileSize(Long.parseLong(fileData.get("size").toString()));
                fileRecord.setFileHash(fileData.get("hash").toString());
                fileRecord.setCategory(fileData.get("category") != null ? fileData.get("category").toString() : "Uncategorized");
                fileRecord.setFileExtension(fileData.get("extension") != null ? fileData.get("extension").toString() : "");
                fileRecord.setCreatedDate(LocalDateTime.now());
                fileRecord.setScannedDate(LocalDateTime.now());
                
                // Save to database
                fileRepository.save(fileRecord);
                savedCount++;
                
                // Log the save operation
                loggingService.logInfo("File saved to database", "SAVE", 
                    "File: " + fileRecord.getFileName() + ", Hash: " + fileRecord.getFileHash());
                    
            } catch (Exception e) {
                loggingService.logError("Failed to save file", "SAVE_ERROR", 
                    "File: " + fileData.get("name") + ", Error: " + e.getMessage());
            }
        }
        return savedCount;
    }

    private String calculateFileHash(Path filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = md.digest(fileBytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}