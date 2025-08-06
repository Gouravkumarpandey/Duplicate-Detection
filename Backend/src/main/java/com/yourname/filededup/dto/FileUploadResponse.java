package com.yourname.filededup.dto;

import com.yourname.filededup.model.FileModel;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for file upload response
 */
public class FileUploadResponse {
    
    private boolean success;
    private String message;
    private boolean isDuplicate;
    private String fileId;
    private String filename;
    private String fileType;
    private String hash;
    private Long fileSize;
    private LocalDateTime uploadDate;
    private String filePath;
    private List<FileModel> existingFiles; // List of files with same hash
    private int duplicateCount;

    // Default constructor
    public FileUploadResponse() {}

    // Constructor for successful upload
    public FileUploadResponse(boolean success, String message, boolean isDuplicate, FileModel fileModel) {
        this.success = success;
        this.message = message;
        this.isDuplicate = isDuplicate;
        this.fileId = fileModel.getId();
        this.filename = fileModel.getFilename();
        this.fileType = fileModel.getFileType();
        this.hash = fileModel.getHash();
        this.fileSize = fileModel.getFileSize();
        this.uploadDate = fileModel.getUploadDate();
        this.filePath = fileModel.getFilePath();
    }

    // Constructor for error response
    public FileUploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Static factory methods
    public static FileUploadResponse success(String message, boolean isDuplicate, FileModel fileModel) {
        return new FileUploadResponse(true, message, isDuplicate, fileModel);
    }

    public static FileUploadResponse error(String message) {
        return new FileUploadResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<FileModel> getExistingFiles() {
        return existingFiles;
    }

    public void setExistingFiles(List<FileModel> existingFiles) {
        this.existingFiles = existingFiles;
        this.duplicateCount = existingFiles != null ? existingFiles.size() : 0;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    @Override
    public String toString() {
        return "FileUploadResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", isDuplicate=" + isDuplicate +
                ", fileId='" + fileId + '\'' +
                ", filename='" + filename + '\'' +
                ", fileType='" + fileType + '\'' +
                ", hash='" + hash + '\'' +
                ", fileSize=" + fileSize +
                ", uploadDate=" + uploadDate +
                ", filePath='" + filePath + '\'' +
                ", duplicateCount=" + duplicateCount +
                '}';
    }
}
