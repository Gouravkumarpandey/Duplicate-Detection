package com.yourname.filededup.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection = "file_records")
public class FileRecord {
    
    @Id
    private String id;
    
    @Indexed
    private String fileName;
    
    private String filePath;
    
    @Indexed
    private String fileHash;
    
    private long fileSize;
    
    private String fileExtension;
    
    @Indexed
    private String category;
    
    private LocalDateTime createdDate;
    
    private LocalDateTime modifiedDate;
    
    private LocalDateTime scannedDate;
    
    private boolean isDuplicate;
    
    private String mimeType;
    
    // New fields for enhanced functionality
    @Indexed
    private String uploadedBy;
    
    private String storedFileName;
    
    private String storageLocation;
    
    private String description;
    
    private String checksum;
    
    private String checksumAlgorithm;
    
    private boolean isVerified;
    
    private LocalDateTime lastVerifiedDate;
    
    private LocalDateTime uploadedDate;
    
    private String tags;
    
    private int duplicateCount;
    
    private String duplicateGroupId;
    
    private boolean isActive;
    
    private String fileVersion;
    
    private long downloadCount;
    
    private LocalDateTime lastAccessDate;
    
    private String contentPreview;
    
    private boolean hasPreview;
    
    private String compressionType;
    
    private boolean isEncrypted;

    // Default constructor
    public FileRecord() {
        this.scannedDate = LocalDateTime.now();
        this.uploadedDate = LocalDateTime.now();
        this.isActive = true;
        this.isVerified = false;
        this.downloadCount = 0;
        this.duplicateCount = 0;
        this.hasPreview = false;
        this.isEncrypted = false;
        this.checksumAlgorithm = "SHA-256";
    }

    // Constructor with essential fields
    public FileRecord(String fileName, String filePath, String fileHash, long fileSize) {
        this();
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.fileExtension = getExtensionFromFileName(fileName);
        this.checksum = fileHash; // Default checksum to file hash
    }

    // Constructor with user information
    public FileRecord(String fileName, String filePath, String fileHash, long fileSize, String uploadedBy) {
        this(fileName, filePath, fileHash, fileSize);
        this.uploadedBy = uploadedBy;
    }

    // Getters and Setters for existing fields
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.fileExtension = getExtensionFromFileName(fileName);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
        if (this.checksum == null || this.checksum.isEmpty()) {
            this.checksum = fileHash;
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public LocalDateTime getScannedDate() {
        return scannedDate;
    }

    public void setScannedDate(LocalDateTime scannedDate) {
        this.scannedDate = scannedDate;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    // Getters and Setters for new fields
    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public void setChecksumAlgorithm(String checksumAlgorithm) {
        this.checksumAlgorithm = checksumAlgorithm;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
        if (verified) {
            this.lastVerifiedDate = LocalDateTime.now();
        }
    }

    public LocalDateTime getLastVerifiedDate() {
        return lastVerifiedDate;
    }

    public void setLastVerifiedDate(LocalDateTime lastVerifiedDate) {
        this.lastVerifiedDate = lastVerifiedDate;
    }

    public LocalDateTime getUploadedDate() {
        return uploadedDate;
    }

    public void setUploadedDate(LocalDateTime uploadedDate) {
        this.uploadedDate = uploadedDate;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public String getDuplicateGroupId() {
        return duplicateGroupId;
    }

    public void setDuplicateGroupId(String duplicateGroupId) {
        this.duplicateGroupId = duplicateGroupId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(String fileVersion) {
        this.fileVersion = fileVersion;
    }

    public long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
        this.lastAccessDate = LocalDateTime.now();
    }

    public LocalDateTime getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(LocalDateTime lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public void setContentPreview(String contentPreview) {
        this.contentPreview = contentPreview;
        this.hasPreview = (contentPreview != null && !contentPreview.trim().isEmpty());
    }

    public boolean isHasPreview() {
        return hasPreview;
    }

    public void setHasPreview(boolean hasPreview) {
        this.hasPreview = hasPreview;
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    // Utility methods
    private String getExtensionFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    // Helper method to get human-readable file size
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    // Helper method to check if file is recently uploaded (within 24 hours)
    public boolean isRecentlyUploaded() {
        if (uploadedDate == null) return false;
        return uploadedDate.isAfter(LocalDateTime.now().minusDays(1));
    }

    // Helper method to get file age in days
    public long getFileAgeDays() {
        if (uploadedDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(uploadedDate, LocalDateTime.now());
    }

    // Helper method to check if verification is needed
    public boolean needsVerification() {
        if (!isVerified) return true;
        if (lastVerifiedDate == null) return true;
        return lastVerifiedDate.isBefore(LocalDateTime.now().minusDays(30)); // Verify monthly
    }

    // Helper method to update access timestamp
    public void updateLastAccess() {
        this.lastAccessDate = LocalDateTime.now();
    }

    // Helper method to mark as duplicate with group ID
    public void markAsDuplicate(String groupId, int count) {
        this.isDuplicate = true;
        this.duplicateGroupId = groupId;
        this.duplicateCount = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileRecord that = (FileRecord) o;
        return Objects.equals(fileHash, that.fileHash) &&
               Objects.equals(fileName, that.fileName) &&
               fileSize == that.fileSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileHash, fileName, fileSize);
    }

    @Override
    public String toString() {
        return "FileRecord{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileHash='" + fileHash + '\'' +
                ", fileSize=" + fileSize +
                ", formattedSize='" + getFormattedFileSize() + '\'' +
                ", category='" + category + '\'' +
                ", uploadedBy='" + uploadedBy + '\'' +
                ", isDuplicate=" + isDuplicate +
                ", duplicateCount=" + duplicateCount +
                ", isVerified=" + isVerified +
                ", uploadedDate=" + uploadedDate +
                ", isActive=" + isActive +
                '}';
    }

    // Method to create a summary for API responses
    public String toSummary() {
        return String.format("File: %s (%s) - %s - Uploaded by: %s on %s", 
                fileName, 
                getFormattedFileSize(), 
                isDuplicate ? "DUPLICATE" : "UNIQUE",
                uploadedBy != null ? uploadedBy : "Unknown",
                uploadedDate != null ? uploadedDate.toLocalDate() : "Unknown");
    }
}