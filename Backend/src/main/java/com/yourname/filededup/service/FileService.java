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

    public List<FileRecord> getAllFiles() {
        return fileRepository.findAll();
    }

    public void deleteFile(String id) throws IOException {
        Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
        if (fileRecordOpt.isPresent()) {
            FileRecord fileRecord = fileRecordOpt.get();
            
            // Delete physical file
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
        
        // Calculate file hash
        String hash = calculateFileHash(filePath);
        fileRecord.setFileHash(hash);
        
        // Set MIME type
        String mimeType = Files.probeContentType(filePath);
        fileRecord.setMimeType(mimeType);
        
        return fileRecord;
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
