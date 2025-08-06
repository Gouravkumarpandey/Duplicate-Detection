package com.yourname.filededup.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

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

    // Default constructor
    public FileRecord() {
        this.scannedDate = LocalDateTime.now();
    }

    // Constructor with essential fields
    public FileRecord(String fileName, String filePath, String fileHash, long fileSize) {
        this();
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileHash = fileHash;
        this.fileSize = fileSize;
        this.fileExtension = getExtensionFromFileName(fileName);
    }

    // Getters and Setters
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

    // Utility method to extract file extension
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

    @Override
    public String toString() {
        return "FileRecord{" +
                "id='" + id + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileHash='" + fileHash + '\'' +
                ", fileSize=" + fileSize +
                ", category='" + category + '\'' +
                ", isDuplicate=" + isDuplicate +
                '}';
    }
}
