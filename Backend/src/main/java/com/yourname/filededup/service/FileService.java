package com.yourname.filededup.service;

import com.yourname.filededup.model.FileRecord;
import com.yourname.filededup.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
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

    // Directory for storing uploaded files
    private static final String UPLOAD_DIRECTORY = "uploads/";
    private static final String TEMP_DIRECTORY = "temp/";

    // ============ CORE FILE PROCESSING ============

    public FileRecord processUploadedFileWithCredentials(MultipartFile file, String email, String password) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        // Log the upload attempt with user credentials
        loggingService.logInfo("File upload initiated", "UPLOAD", 
            "User: " + email + ", File: " + fileName + ", Size: " + file.getSize() + " bytes");

        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(UPLOAD_DIRECTORY);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate unique filename to avoid conflicts
        String uniqueFileName = generateUniqueFileName(fileName, email);
        Path filePath = uploadDir.resolve(uniqueFileName);
        
        // Save the file
        file.transferTo(filePath.toFile());

        try {
            FileRecord fileRecord = createFileRecord(filePath);
            fileRecord.setFileName(fileName);
            fileRecord.setStoredFileName(uniqueFileName);
            fileRecord.setStorageLocation(filePath.toString());
            fileRecord.setUploadedBy(email);
            fileRecord.setUploadedDate(LocalDateTime.now());
            fileRecord.setScannedDate(LocalDateTime.now());
            fileRecord.setMimeType(file.getContentType());
            
            // Categorize the file
            String category = ruleEngineService.categorizeFile(fileRecord);
            fileRecord.setCategory(category);
            
            // Check for duplicates and create duplicate groups
            handleDuplicateDetection(fileRecord);
            
            FileRecord savedFile = fileRepository.save(fileRecord);
            
            loggingService.logInfo("File uploaded and processed successfully", "UPLOAD", 
                "User: " + email + ", File: " + fileName + ", Category: " + category + ", ID: " + savedFile.getId());
            
            return savedFile;
            
        } catch (Exception e) {
            // Clean up file on error
            Files.deleteIfExists(filePath);
            throw e;
        }
    }

    public Map<String, Object> processMultipleFiles(MultipartFile[] files, String email, String password) throws IOException {
        List<FileRecord> processedFiles = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int duplicateCount = 0;

        for (MultipartFile file : files) {
            try {
                FileRecord processedFile = processUploadedFileWithCredentials(file, email, password);
                processedFiles.add(processedFile);
                if (processedFile.isDuplicate()) {
                    duplicateCount++;
                }
            } catch (Exception e) {
                errors.add("Failed to process " + file.getOriginalFilename() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("totalFiles", files.length);
        result.put("processedFiles", processedFiles.size());
        result.put("duplicateCount", duplicateCount);
        result.put("errorCount", errors.size());
        result.put("files", processedFiles);
        result.put("errors", errors);

        return result;
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
        int skippedFiles = 0;

        try (Stream<Path> paths = Files.walk(path)) {
            List<Path> filePaths = paths
                .filter(Files::isRegularFile)
                .filter(this::isValidFileForScanning)
                .collect(Collectors.toList());

            totalFiles = filePaths.size();

            for (Path filePath : filePaths) {
                try {
                    // Check if file already exists in database
                    if (fileRepository.existsByFilePath(filePath.toString())) {
                        skippedFiles++;
                        continue;
                    }

                    FileRecord fileRecord = createFileRecord(filePath);
                    fileRecord.setUploadedBy("SYSTEM_SCAN");
                    
                    // Categorize the file
                    String category = ruleEngineService.categorizeFile(fileRecord);
                    fileRecord.setCategory(category);
                    
                    // Handle duplicate detection
                    handleDuplicateDetection(fileRecord);
                    if (fileRecord.isDuplicate()) {
                        duplicateCount++;
                    }
                    
                    scannedFiles.add(fileRepository.save(fileRecord));
                    
                } catch (Exception e) {
                    loggingService.logError("Failed to process file", "SCAN", 
                        "File: " + filePath + ", Error: " + e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", totalFiles);
        result.put("scannedFiles", scannedFiles.size());
        result.put("skippedFiles", skippedFiles);
        result.put("duplicateCount", duplicateCount);
        result.put("files", scannedFiles);

        loggingService.logInfo("Directory scan completed", "SCAN", 
            "Processed: " + scannedFiles.size() + " files, Duplicates: " + duplicateCount + ", Skipped: " + skippedFiles);

        return result;
    }

    // ============ FILE RETRIEVAL WITH ADVANCED FILTERING ============

    public Map<String, Object> getFilesWithFilters(Boolean duplicate, String uploadedBy, String category, 
                                                   int page, int size, String sortBy, String sortOrder) {
        
        Pageable pageable = PageRequest.of(page, size, 
            sortOrder.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
        
        List<FileRecord> allFiles = fileRepository.findAll();
        
        // Apply filters
        Stream<FileRecord> filteredStream = allFiles.stream();
        
        if (duplicate != null) {
            filteredStream = filteredStream.filter(file -> file.isDuplicate() == duplicate);
        }
        if (uploadedBy != null && !uploadedBy.trim().isEmpty()) {
            filteredStream = filteredStream.filter(file -> 
                file.getUploadedBy() != null && file.getUploadedBy().toLowerCase().contains(uploadedBy.toLowerCase()));
        }
        if (category != null && !category.trim().isEmpty()) {
            filteredStream = filteredStream.filter(file -> 
                Objects.equals(file.getCategory(), category));
        }
        
        List<FileRecord> filteredFiles = filteredStream.collect(Collectors.toList());
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredFiles.size());
        List<FileRecord> pageContent = filteredFiles.subList(start, end);
        
        Page<FileRecord> filePage = new PageImpl<>(pageContent, pageable, filteredFiles.size());
        
        Map<String, Object> result = new HashMap<>();
        result.put("files", filePage.getContent());
        result.put("totalElements", filePage.getTotalElements());
        result.put("totalPages", filePage.getTotalPages());
        result.put("currentPage", filePage.getNumber());
        result.put("pageSize", filePage.getSize());
        result.put("hasNext", filePage.hasNext());
        result.put("hasPrevious", filePage.hasPrevious());
        
        return result;
    }

    public Map<String, Object> searchFilesAdvanced(String fileName, String category, Long minSize, Long maxSize, 
                                                   String uploadedBy, String tags, int page, int size) {
        
        List<FileRecord> allFiles = fileRepository.findAll();
        
        Stream<FileRecord> filteredStream = allFiles.stream()
            .filter(file -> fileName == null || fileName.isEmpty() || 
                    file.getFileName().toLowerCase().contains(fileName.toLowerCase()))
            .filter(file -> category == null || category.isEmpty() || 
                    Objects.equals(file.getCategory(), category))
            .filter(file -> minSize == null || file.getFileSize() >= minSize)
            .filter(file -> maxSize == null || file.getFileSize() <= maxSize)
            .filter(file -> uploadedBy == null || uploadedBy.isEmpty() || 
                    (file.getUploadedBy() != null && file.getUploadedBy().toLowerCase().contains(uploadedBy.toLowerCase())))
            .filter(file -> tags == null || tags.isEmpty() || 
                    (file.getTags() != null && file.getTags().toLowerCase().contains(tags.toLowerCase())));
        
        List<FileRecord> filteredFiles = filteredStream.collect(Collectors.toList());
        
        // Apply pagination
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredFiles.size());
        List<FileRecord> pageContent = filteredFiles.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("files", pageContent);
        result.put("totalElements", filteredFiles.size());
        result.put("totalPages", (int) Math.ceil((double) filteredFiles.size() / size));
        result.put("currentPage", page);
        result.put("pageSize", size);
        
        return result;
    }

    // ============ DUPLICATE MANAGEMENT ============

    public Map<String, Object> getDuplicatesWithPagination(int page, int size) {
        List<FileRecord> duplicates = fileRepository.findByIsDuplicate(true);
        
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), duplicates.size());
        List<FileRecord> pageContent = duplicates.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("duplicates", pageContent);
        result.put("totalElements", duplicates.size());
        result.put("totalPages", (int) Math.ceil((double) duplicates.size() / size));
        result.put("currentPage", page);
        result.put("pageSize", size);
        
        return result;
    }

    public Map<String, Object> getDuplicateGroups() {
        List<FileRecord> duplicates = fileRepository.findByIsDuplicate(true);
        
        Map<String, List<FileRecord>> groups = duplicates.stream()
            .filter(file -> file.getDuplicateGroupId() != null)
            .collect(Collectors.groupingBy(FileRecord::getDuplicateGroupId));
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalGroups", groups.size());
        result.put("totalDuplicates", duplicates.size());
        result.put("groups", groups);
        
        return result;
    }

    public Map<String, Object> resolveDuplicates(Map<String, Object> resolutionData) {
        String strategy = (String) resolutionData.get("strategy");
        @SuppressWarnings("unchecked")
        List<String> fileIds = (List<String>) resolutionData.get("fileIds");
        
        int resolvedCount = 0;
        List<String> errors = new ArrayList<>();
        
        switch (strategy.toLowerCase()) {
            case "delete_all":
                resolvedCount = deleteMultipleFiles(fileIds, errors);
                break;
            case "keep_newest":
                resolvedCount = keepNewestInGroup(fileIds, errors);
                break;
            case "keep_largest":
                resolvedCount = keepLargestInGroup(fileIds, errors);
                break;
            default:
                errors.add("Unknown resolution strategy: " + strategy);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("resolvedCount", resolvedCount);
        result.put("errors", errors);
        result.put("strategy", strategy);
        
        return result;
    }

    public Map<String, Object> rescanForDuplicates() {
        List<FileRecord> allFiles = fileRepository.findAll();
        Map<String, List<FileRecord>> hashGroups = allFiles.stream()
            .collect(Collectors.groupingBy(FileRecord::getFileHash));
        
        int newDuplicatesFound = 0;
        int groupsProcessed = 0;
        
        for (Map.Entry<String, List<FileRecord>> entry : hashGroups.entrySet()) {
            List<FileRecord> filesWithSameHash = entry.getValue();
            if (filesWithSameHash.size() > 1) {
                String groupId = UUID.randomUUID().toString();
                for (FileRecord file : filesWithSameHash) {
                    if (!file.isDuplicate()) {
                        newDuplicatesFound++;
                    }
                    file.markAsDuplicate(groupId, filesWithSameHash.size());
                    fileRepository.save(file);
                }
                groupsProcessed++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", allFiles.size());
        result.put("duplicateGroups", groupsProcessed);
        result.put("newDuplicatesFound", newDuplicatesFound);
        result.put("message", "Duplicate rescan completed successfully");
        
        return result;
    }

    // ============ USER MANAGEMENT ============

    public Map<String, Object> getFilesByUserWithPagination(String email, int page, int size) {
        List<FileRecord> userFiles = fileRepository.findByUploadedBy(email);
        
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), userFiles.size());
        List<FileRecord> pageContent = userFiles.subList(start, end);
        
        Map<String, Object> result = new HashMap<>();
        result.put("files", pageContent);
        result.put("totalElements", userFiles.size());
        result.put("totalPages", (int) Math.ceil((double) userFiles.size() / size));
        result.put("currentPage", page);
        result.put("userEmail", email);
        
        return result;
    }

    public Map<String, Object> getUserStatistics(String email) {
        List<FileRecord> userFiles = fileRepository.findByUploadedBy(email);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", userFiles.size());
        stats.put("duplicateFiles", userFiles.stream().mapToLong(f -> f.isDuplicate() ? 1 : 0).sum());
        stats.put("totalSize", userFiles.stream().mapToLong(FileRecord::getFileSize).sum());
        stats.put("averageFileSize", userFiles.isEmpty() ? 0 : 
            userFiles.stream().mapToLong(FileRecord::getFileSize).average().orElse(0));
        
        // Category breakdown for user
        Map<String, Long> categoryBreakdown = userFiles.stream()
            .collect(Collectors.groupingBy(
                file -> file.getCategory() != null ? file.getCategory() : "Other",
                Collectors.counting()
            ));
        stats.put("categoryBreakdown", categoryBreakdown);
        
        // Recent activity
        long recentFiles = userFiles.stream()
            .mapToLong(f -> f.isRecentlyUploaded() ? 1 : 0).sum();
        stats.put("recentFiles", recentFiles);
        
        stats.put("userEmail", email);
        
        return stats;
    }

    public Map<String, Object> getAllUsersWithStats() {
        List<FileRecord> allFiles = fileRepository.findAll();
        
        Map<String, List<FileRecord>> userFiles = allFiles.stream()
            .filter(file -> file.getUploadedBy() != null)
            .collect(Collectors.groupingBy(FileRecord::getUploadedBy));
        
        List<Map<String, Object>> userStats = new ArrayList<>();
        
        for (Map.Entry<String, List<FileRecord>> entry : userFiles.entrySet()) {
            String userEmail = entry.getKey();
            List<FileRecord> files = entry.getValue();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("email", userEmail);
            stats.put("totalFiles", files.size());
            stats.put("duplicateFiles", files.stream().mapToLong(f -> f.isDuplicate() ? 1 : 0).sum());
            stats.put("totalSize", files.stream().mapToLong(FileRecord::getFileSize).sum());
            stats.put("lastUpload", files.stream()
                .map(FileRecord::getUploadedDate)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
            
            userStats.add(stats);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userStats.size());
        result.put("users", userStats);
        
        return result;
    }

    // ============ DELETE OPERATIONS ============

    public boolean deleteFile(String id) {
        Optional<FileRecord> fileRecordOpt = fileRepository.findById(id);
        if (fileRecordOpt.isPresent()) {
            FileRecord fileRecord = fileRecordOpt.get();
            
            try {
                // Delete physical file if it exists
                if (fileRecord.getStorageLocation() != null) {
                    Path filePath = Paths.get(fileRecord.getStorageLocation());
                    Files.deleteIfExists(filePath);
                }
                
                // Delete from database
                fileRepository.deleteById(id);
                
                loggingService.logInfo("File deleted", "DELETE", 
                    "File: " + fileRecord.getFileName() + " (" + fileRecord.getStorageLocation() + ")");
                
                return true;
            } catch (IOException e) {
                loggingService.logError("Failed to delete physical file", "DELETE", 
                    "File: " + fileRecord.getFileName() + ", Error: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public Map<String, Object> deleteDuplicatesAdvanced(boolean keepOldest) {
        Map<String, List<FileRecord>> duplicateGroups = fileRepository.findByIsDuplicate(true).stream()
            .filter(file -> file.getDuplicateGroupId() != null)
            .collect(Collectors.groupingBy(FileRecord::getDuplicateGroupId));
        
        int deletedCount = 0;
        int groupsProcessed = 0;
        List<String> errors = new ArrayList<>();
        
        for (List<FileRecord> group : duplicateGroups.values()) {
            if (group.size() > 1) {
                // Sort by upload date
                group.sort(Comparator.comparing(FileRecord::getUploadedDate, 
                    Comparator.nullsLast(Comparator.naturalOrder())));
                
                // Keep either oldest or newest based on strategy
                List<FileRecord> toDelete = keepOldest ? 
                    group.subList(1, group.size()) : 
                    group.subList(0, group.size() - 1);
                
                for (FileRecord file : toDelete) {
                    if (deleteFile(file.getId())) {
                        deletedCount++;
                    } else {
                        errors.add("Failed to delete file: " + file.getFileName());
                    }
                }
                groupsProcessed++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("groupsProcessed", groupsProcessed);
        result.put("deletedCount", deletedCount);
        result.put("strategy", keepOldest ? "keep_oldest" : "keep_newest");
        result.put("errors", errors);
        
        return result;
    }

    public Map<String, Object> batchDeleteFilesAdvanced(List<String> fileIds, boolean deletePhysical) {
        int deletedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (String fileId : fileIds) {
            try {
                Optional<FileRecord> fileOpt = fileRepository.findById(fileId);
                if (fileOpt.isPresent()) {
                    FileRecord file = fileOpt.get();
                    
                    if (deletePhysical && file.getStorageLocation() != null) {
                        Path filePath = Paths.get(file.getStorageLocation());
                        Files.deleteIfExists(filePath);
                    }
                    
                    fileRepository.deleteById(fileId);
                    deletedCount++;
                    
                    loggingService.logInfo("File deleted in batch operation", "DELETE", 
                        "File: " + file.getFileName());
                } else {
                    errors.add("File not found: " + fileId);
                }
            } catch (Exception e) {
                errors.add("Failed to delete file " + fileId + ": " + e.getMessage());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("totalRequested", fileIds.size());
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        result.put("physicalDeletion", deletePhysical);
        
        return result;
    }

    public Map<String, Object> deleteFilesByUser(String email) {
        List<FileRecord> userFiles = fileRepository.findByUploadedBy(email);
        int deletedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (FileRecord file : userFiles) {
            if (deleteFile(file.getId())) {
                deletedCount++;
            } else {
                errors.add("Failed to delete file: " + file.getFileName());
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("userEmail", email);
        result.put("totalFiles", userFiles.size());
        result.put("deletedCount", deletedCount);
        result.put("errors", errors);
        
        return result;
    }

    // ============ STATISTICS & ANALYTICS ============

    public Map<String, Object> getFileStatistics() {
        List<FileRecord> allFiles = fileRepository.findAll();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", allFiles.size());
        stats.put("duplicateFiles", allFiles.stream().mapToLong(f -> f.isDuplicate() ? 1 : 0).sum());
        stats.put("totalSize", allFiles.stream().mapToLong(FileRecord::getFileSize).sum());
        stats.put("averageSize", allFiles.isEmpty() ? 0 : 
            allFiles.stream().mapToLong(FileRecord::getFileSize).average().orElse(0));
        
        // Category breakdown
        Map<String, Long> categoryStats = getCategoryStatistics();
        stats.put("categoryBreakdown", categoryStats);
        
        // Size breakdown
        Map<String, Long> sizeBreakdown = calculateSizeBreakdown(allFiles);
        stats.put("sizeBreakdown", sizeBreakdown);
        
        // Recent activity
        long recentFiles = allFiles.stream()
            .mapToLong(f -> f.isRecentlyUploaded() ? 1 : 0).sum();
        stats.put("recentFiles", recentFiles);
        
        return stats;
    }

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> dashboard = getFileStatistics();
        
        // Add additional dashboard-specific metrics
        List<FileRecord> allFiles = fileRepository.findAll();
        
        // Top uploaders
        Map<String, Long> topUploaders = allFiles.stream()
            .filter(file -> file.getUploadedBy() != null)
            .collect(Collectors.groupingBy(FileRecord::getUploadedBy, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        dashboard.put("topUploaders", topUploaders);
        
        // Storage efficiency
        long duplicateSize = allFiles.stream()
            .filter(FileRecord::isDuplicate)
            .mapToLong(FileRecord::getFileSize)
            .sum();
        dashboard.put("duplicateSize", duplicateSize);
        dashboard.put("storageEfficiency", calculateStorageEfficiency(allFiles));
        
        return dashboard;
    }

    public Map<String, Object> getCategoryStatistics() {
        List<FileRecord> allFiles = fileRepository.findAll();
        
        Map<String, Long> categoryCounts = allFiles.stream()
            .collect(Collectors.groupingBy(
                file -> file.getCategory() != null ? file.getCategory() : "Other",
                Collectors.counting()
            ));
        
        Map<String, Long> categorySizes = allFiles.stream()
            .collect(Collectors.groupingBy(
                file -> file.getCategory() != null ? file.getCategory() : "Other",
                Collectors.summingLong(FileRecord::getFileSize)
            ));
        
        Map<String, Object> result = new HashMap<>();
        result.put("counts", categoryCounts);
        result.put("sizes", categorySizes);
        result.put("totalCategories", categoryCounts.size());
        
        return result;
    }

    public List<FileRecord> getRecentFiles(int hours, int limit) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        
        return fileRepository.findAll().stream()
            .filter(file -> file.getUploadedDate() != null && file.getUploadedDate().isAfter(cutoff))
            .sorted(Comparator.comparing(FileRecord::getUploadedDate).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    public Map<String, Object> getFileSizeAnalysis() {
        List<FileRecord> allFiles = fileRepository.findAll();
        
        long totalSize = allFiles.stream().mapToLong(FileRecord::getFileSize).sum();
        OptionalDouble averageSize = allFiles.stream().mapToLong(FileRecord::getFileSize).average();
        
        Map<String, Object> analysis = new HashMap<>();
        analysis.put("totalSize", totalSize);
        analysis.put("averageSize", averageSize.orElse(0));
        analysis.put("largestFile", allFiles.stream().max(Comparator.comparing(FileRecord::getFileSize)).orElse(null));
        analysis.put("smallestFile", allFiles.stream().min(Comparator.comparing(FileRecord::getFileSize)).orElse(null));
        analysis.put("sizeDistribution", calculateSizeBreakdown(allFiles));
        
        return analysis;
    }

    public Map<String, Object> getUploadTrends(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        
        Map<String, Long> dailyUploads = fileRepository.findAll().stream()
            .filter(file -> file.getUploadedDate() != null && file.getUploadedDate().isAfter(cutoff))
            .collect(Collectors.groupingBy(
                file -> file.getUploadedDate().toLocalDate().toString(),
                Collectors.counting()
            ));
        
        Map<String, Object> trends = new HashMap<>();
        trends.put("period", days + " days");
        trends.put("dailyUploads", dailyUploads);
        trends.put("totalUploadsInPeriod", dailyUploads.values().stream().mapToLong(Long::longValue).sum());
        
        return trends;
    }

    // ============ FILE INTEGRITY & VERIFICATION ============

    public Map<String, Object> verifyFileIntegrity(String fileId) {
        Optional<FileRecord> fileOpt = fileRepository.findById(fileId);
        if (!fileOpt.isPresent()) {
            throw new IllegalArgumentException("File not found with id: " + fileId);
        }
        
        FileRecord file = fileOpt.get();
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (file.getStorageLocation() != null) {
                Path filePath = Paths.get(file.getStorageLocation());
                if (Files.exists(filePath)) {
                    String currentHash = calculateFileHash(filePath);
                    boolean isValid = currentHash.equals(file.getFileHash());
                    
                    file.setVerified(isValid);
                    file.setLastVerifiedDate(LocalDateTime.now());
                    fileRepository.save(file);
                    
                    result.put("fileId", fileId);
                    result.put("fileName", file.getFileName());
                    result.put("isValid", isValid);
                    result.put("originalHash", file.getFileHash());
                    result.put("currentHash", currentHash);
                    result.put("verifiedAt", LocalDateTime.now());
                } else {
                    result.put("error", "Physical file not found");
                    result.put("isValid", false);
                }
            } else {
                result.put("error", "No storage location recorded");
                result.put("isValid", false);
            }
        } catch (Exception e) {
            result.put("error", "Verification failed: " + e.getMessage());
            result.put("isValid", false);
        }
        
        return result;
    }

    public Map<String, Object> verifyAllFiles() {
        List<FileRecord> allFiles = fileRepository.findAll();
        AtomicInteger validCount = new AtomicInteger(0);
        AtomicInteger invalidCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        List<CompletableFuture<Void>> futures = allFiles.stream()
            .map(file -> CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> result = verifyFileIntegrity(file.getId());
                    boolean isValid = (Boolean) result.getOrDefault("isValid", false);
                    if (isValid) {
                        validCount.incrementAndGet();
                    } else {
                        invalidCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            }))
            .collect(Collectors.toList());
        
        // Wait for all verifications to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalFiles", allFiles.size());
        result.put("validFiles", validCount.get());
        result.put("invalidFiles", invalidCount.get());
        result.put("errorCount", errorCount.get());
        result.put("verificationDate", LocalDateTime.now());
        
        return result;
    }

    // ============ MAINTENANCE & CLEANUP ============

    public Map<String, Object> cleanupOrphanedFiles() {
        List<FileRecord> allRecords = fileRepository.findAll();
        int orphanedRecords = 0;
        int orphanedFiles = 0;
        List<String> errors = new ArrayList<>();
        
        // Find database records without physical files
        for (FileRecord record : allRecords) {
            if (record.getStorageLocation() != null) {
                Path filePath = Paths.get(record.getStorageLocation());
                if (!Files.exists(filePath)) {
                    try {
                        fileRepository.deleteById(record.getId());
                        orphanedRecords++;
                    } catch (Exception e) {
                        errors.add("Failed to delete orphaned record: " + record.getFileName());
                    }
                }
            }
        }
        
        // Find physical files without database records
        try {
            Path uploadDir = Paths.get(UPLOAD_DIRECTORY);
            if (Files.exists(uploadDir)) {
                Set<String> recordedPaths = allRecords.stream()
                    .map(FileRecord::getStorageLocation)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                
                try (Stream<Path> files = Files.walk(uploadDir)) {
                    files.filter(Files::isRegularFile)
                         .filter(path -> !recordedPaths.contains(path.toString()))
                         .forEach(path -> {
                             try {
                                 Files.delete(path);
                                 orphanedFiles++;
                             } catch (IOException e) {
                                 errors.add("Failed to delete orphaned file: " + path.toString());
                             }
                         });
                }
            }
        } catch (IOException e) {
            errors.add("Failed to scan upload directory: " + e.getMessage());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("orphanedRecords", orphanedRecords);
        result.put("orphanedFiles", orphanedFiles);
        result.put("errors", errors);
        result.put("cleanupDate", LocalDateTime.now());
        
        return result;
    }

    public Map<String, Object> optimizeStorage() {
        Map<String, Object> result = new HashMap<>();
        
        // Clean up orphaned files first
        Map<String, Object> cleanupResult = cleanupOrphanedFiles();
        result.put("cleanup", cleanupResult);
        
        // Compress or move old files (placeholder for actual implementation)
        List<FileRecord> oldFiles = fileRepository.findAll().stream()
            .filter(file -> file.getFileAgeDays() > 365)
            .collect(Collectors.toList());
        
        result.put("oldFilesFound", oldFiles.size());
        result.put("optimizationDate", LocalDateTime.now());
        result.put("message", "Storage optimization completed");
        
        return result;
    }

    // ============ EXPORT & IMPORT ============

    public Map<String, Object> exportFileList(String format, Boolean duplicatesOnly, String category, String uploadedBy) {
        List<FileRecord> files = fileRepository.findAll();
        
        // Apply filters
        if (duplicatesOnly != null && duplicatesOnly) {
            files = files.stream().filter(FileRecord::isDuplicate).collect(Collectors.toList());
        }
        if (category != null && !category.trim().isEmpty()) {
            files = files.stream().filter(file -> Objects.equals(file.getCategory(), category)).collect(Collectors.toList());
        }
        if (uploadedBy != null && !uploadedBy.trim().isEmpty()) {
            files = files.stream().filter(file -> 
                file.getUploadedBy() != null && file.getUploadedBy().contains(uploadedBy)).collect(Collectors.toList());
        }
        
        String exportData;
        switch (format.toLowerCase()) {
            case "csv":
                exportData = generateCsvExport(files);
                break;
            case "xlsx":
                exportData = generateXlsxExport(files);
                break;
            default:
                exportData = generateJsonExport(files);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("format", format);
        result.put("totalFiles", files.size());
        result.put("exportData", exportData);
        result.put("exportDate", LocalDateTime.now());
        result.put("filters", Map.of(
            "duplicatesOnly", duplicatesOnly != null ? duplicatesOnly : false,
            "category", category != null ? category : "all",
            "uploadedBy", uploadedBy != null ? uploadedBy : "all"
        ));
        
        return result;
    }

    public Map<String, Object> importMetadata(MultipartFile metadataFile) {
        // Placeholder for metadata import functionality
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Metadata import functionality not yet implemented");
        result.put("fileName", metadataFile.getOriginalFilename());
        result.put("fileSize", metadataFile.getSize());
        
        return result;
    }

    // ============ FILE METADATA UPDATES ============

    public FileRecord updateFileTags(String fileId, String tags) {
        Optional<FileRecord> fileOpt = fileRepository.findById(fileId);
        if (!fileOpt.isPresent()) {
            throw new IllegalArgumentException("File not found with id: " + fileId);
        }
        
        FileRecord file = fileOpt.get();
        String oldTags = file.getTags();
        file.setTags(tags);
        FileRecord updatedFile = fileRepository.save(file);
        
        loggingService.logInfo("File tags updated", "UPDATE", 
            "File: " + file.getFileName() + ", Old tags: " + oldTags + ", New tags: " + tags);
        
        return updatedFile;
    }

    public FileRecord updateFileDescription(String fileId, String description) {
        Optional<FileRecord> fileOpt = fileRepository.findById(fileId);
        if (!fileOpt.isPresent()) {
            throw new IllegalArgumentException("File not found with id: " + fileId);
        }
        
        FileRecord file = fileOpt.get();
        String oldDescription = file.getDescription();
        file.setDescription(description);
        FileRecord updatedFile = fileRepository.save(file);
        
        loggingService.logInfo("File description updated", "UPDATE", 
            "File: " + file.getFileName() + ", Old description: " + oldDescription + ", New description: " + description);
        
        return updatedFile;
    }

    public void updateFile(FileRecord file) {
        fileRepository.save(file);
    }

    public int bulkUpdateCategories(Map<String, String> categoryUpdates) {
        int updatedCount = 0;
        
        for (Map.Entry<String, String> entry : categoryUpdates.entrySet()) {
            try {
                updateFileCategory(entry.getKey(), entry.getValue());
                updatedCount++;
            } catch (Exception e) {
                loggingService.logError("Failed to update category in bulk operation", "UPDATE", 
                    "FileId: " + entry.getKey() + ", Error: " + e.getMessage());
            }
        }
        
        return updatedCount;
    }

    // ============ SYSTEM HEALTH ============

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Database connectivity
            long totalFiles = fileRepository.count();
            health.put("database", "healthy");
            health.put("totalFiles", totalFiles);
            
            // Storage space
            Path uploadDir = Paths.get(UPLOAD_DIRECTORY);
            if (Files.exists(uploadDir)) {
                long totalSpace = Files.getFileStore(uploadDir).getTotalSpace();
                long usableSpace = Files.getFileStore(uploadDir).getUsableSpace();
                health.put("storage", Map.of(
                    "totalSpace", totalSpace,
                    "usableSpace", usableSpace,
                    "usagePercentage", ((totalSpace - usableSpace) * 100.0) / totalSpace
                ));
            }
            
            health.put("status", "healthy");
            health.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
        }
        
        return health;
    }

    // ============ HELPER METHODS ============

    private void handleDuplicateDetection(FileRecord fileRecord) {
        List<FileRecord> duplicates = fileRepository.findByFileHash(fileRecord.getFileHash());
        
        if (!duplicates.isEmpty()) {
            // Create or update duplicate group
            String groupId = duplicates.get(0).getDuplicateGroupId();
            if (groupId == null) {
                groupId = UUID.randomUUID().toString();
                // Update all existing duplicates with group ID
                for (FileRecord duplicate : duplicates) {
                    duplicate.markAsDuplicate(groupId, duplicates.size() + 1);
                    fileRepository.save(duplicate);
                }
            }
            
            // Mark current file as duplicate
            fileRecord.markAsDuplicate(groupId, duplicates.size() + 1);
        }
    }

    private String generateUniqueFileName(String originalFileName, String userEmail) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String userPrefix = userEmail.replaceAll("[^a-zA-Z0-9]", "_");
        
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String name = originalFileName.substring(0, lastDotIndex);
            String extension = originalFileName.substring(lastDotIndex);
            return userPrefix + "_" + timestamp + "_" + name + extension;
        } else {
            return userPrefix + "_" + timestamp + "_" + originalFileName;
        }
    }

    private boolean isValidFileForScanning(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            return fileName.endsWith(".txt") || 
                   fileName.endsWith(".pdf") || 
                   fileName.endsWith(".docx") ||
                   fileName.endsWith(".doc") ||
                   fileName.endsWith(".csv") ||
                   fileName.endsWith(".rtf");
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Long> calculateSizeBreakdown(List<FileRecord> files) {
        Map<String, Long> sizeBreakdown = new HashMap<>();
        sizeBreakdown.put("small", files.stream().mapToLong(f -> f.getFileSize() < 1024 ? 1 : 0).sum());
        sizeBreakdown.put("medium", files.stream().mapToLong(f -> 
            f.getFileSize() >= 1024 && f.getFileSize() < 1024 * 1024 ? 1 : 0).sum());
        sizeBreakdown.put("large", files.stream().mapToLong(f -> 
            f.getFileSize() >= 1024 * 1024 && f.getFileSize() < 100 * 1024 * 1024 ? 1 : 0).sum());
        sizeBreakdown.put("xlarge", files.stream().mapToLong(f -> 
            f.getFileSize() >= 100 * 1024 * 1024 ? 1 : 0).sum());
        return sizeBreakdown;
    }

    private double calculateStorageEfficiency(List<FileRecord> files) {
        long totalSize = files.stream().mapToLong(FileRecord::getFileSize).sum();
        long duplicateSize = files.stream()
            .filter(FileRecord::isDuplicate)
            .mapToLong(FileRecord::getFileSize)
            .sum();
        
        if (totalSize == 0) return 100.0;
        return ((double) (totalSize - duplicateSize) / totalSize) * 100.0;
    }

    private int deleteMultipleFiles(List<String> fileIds, List<String> errors) {
        int deletedCount = 0;
        for (String fileId : fileIds) {
            if (deleteFile(fileId)) {
                deletedCount++;
            } else {
                errors.add("Failed to delete file: " + fileId);
            }
        }
        return deletedCount;
    }

    private int keepNewestInGroup(List<String> fileIds, List<String> errors) {
        List<FileRecord> files = fileIds.stream()
            .map(id -> fileRepository.findById(id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        
        if (files.size() <= 1) return 0;
        
        // Sort by upload date (newest first)
        files.sort(Comparator.comparing(FileRecord::getUploadedDate, 
            Comparator.nullsLast(Comparator.reverseOrder())));
        
        // Delete all except the first (newest)
        List<String> toDelete = files.subList(1, files.size()).stream()
            .map(FileRecord::getId)
            .collect(Collectors.toList());
        
        return deleteMultipleFiles(toDelete, errors);
    }

    private int keepLargestInGroup(List<String> fileIds, List<String> errors) {
        List<FileRecord> files = fileIds.stream()
            .map(id -> fileRepository.findById(id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        
        if (files.size() <= 1) return 0;
        
        // Sort by file size (largest first)
        files.sort(Comparator.comparing(FileRecord::getFileSize).reversed());
        
        // Delete all except the first (largest)
        List<String> toDelete = files.subList(1, files.size()).stream()
            .map(FileRecord::getId)
            .collect(Collectors.toList());
        
        return deleteMultipleFiles(toDelete, errors);
    }

    private String generateJsonExport(List<FileRecord> files) {
        // Simplified JSON export - in real implementation, use proper JSON library
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < files.size(); i++) {
            FileRecord file = files.get(i);
            json.append("{")
                .append("\"id\":\"").append(file.getId()).append("\",")
                .append("\"fileName\":\"").append(file.getFileName()).append("\",")
                .append("\"fileSize\":").append(file.getFileSize()).append(",")
                .append("\"isDuplicate\":").append(file.isDuplicate()).append(",")
                .append("\"category\":\"").append(file.getCategory() != null ? file.getCategory() : "").append("\",")
                .append("\"uploadedBy\":\"").append(file.getUploadedBy() != null ? file.getUploadedBy() : "").append("\"")
                .append("}");
            if (i < files.size() - 1) json.append(",");
        }
        json.append("]");
        return json.toString();
    }

    private String generateCsvExport(List<FileRecord> files) {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,File Name,File Size,Is Duplicate,Category,Uploaded By,Upload Date\n");
        
        for (FileRecord file : files) {
            csv.append(file.getId()).append(",")
               .append("\"").append(file.getFileName()).append("\",")
               .append(file.getFileSize()).append(",")
               .append(file.isDuplicate()).append(",")
               .append("\"").append(file.getCategory() != null ? file.getCategory() : "").append("\",")
               .append("\"").append(file.getUploadedBy() != null ? file.getUploadedBy() : "").append("\",")
               .append("\"").append(file.getUploadedDate() != null ? file.getUploadedDate().toString() : "").append("\"")
               .append("\n");
        }
        
        return csv.toString();
    }

    private String generateXlsxExport(List<FileRecord> files) {
        // Placeholder for XLSX export - would need Apache POI library
        return "XLSX export not implemented - would require Apache POI library";
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
        fileRecord.setChecksum(hash);
        
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

    // ============ EXISTING METHODS (for compatibility) ============

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
}