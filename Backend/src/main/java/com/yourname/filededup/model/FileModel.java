package com.yourname.filededup.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * MongoDB document model for storing file metadata
 * Collection: files
 */
@Document(collection = "files")
public class FileModel {
    
    @Id
    private String id;
    
    @Field("filename")
    private String filename;
    
    @Field("fileType")
    private String fileType;
    
    @Field("hash")
    @Indexed(unique = false) // Allow multiple files with same hash for duplicate tracking
    private String hash;
    
    @Field("uploadDate")
    private LocalDateTime uploadDate;
    
    @Field("fileSize")
    private Long fileSize;
    
    @Field("isDuplicate")
    private Boolean isDuplicate;
    
    @Field("filePath")
    private String filePath; // Local storage path
    
    @Field("mimeType")
    private String mimeType;
    
    @Field("originalName")
    private String originalName;

    // Default constructor
    public FileModel() {
        this.uploadDate = LocalDateTime.now();
        this.isDuplicate = false;
    }

    // Constructor with required fields
    public FileModel(String filename, String fileType, String hash, Long fileSize, Boolean isDuplicate) {
        this();
        this.filename = filename;
        this.fileType = fileType;
        this.hash = hash;
        this.fileSize = fileSize;
        this.isDuplicate = isDuplicate;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Boolean getIsDuplicate() {
        return isDuplicate;
    }

    public void setIsDuplicate(Boolean isDuplicate) {
        this.isDuplicate = isDuplicate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    @Override
    public String toString() {
        return "FileModel{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", fileType='" + fileType + '\'' +
                ", hash='" + hash + '\'' +
                ", uploadDate=" + uploadDate +
                ", fileSize=" + fileSize +
                ", isDuplicate=" + isDuplicate +
                ", filePath='" + filePath + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", originalName='" + originalName + '\'' +
                '}';
    }
}
