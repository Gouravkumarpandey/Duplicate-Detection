package com.yourname.filededup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourname.filededup.model.FileModel;

/**
 * Result DTO for individual file upload within a folder upload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileUploadResult {
    
    private boolean success;
    private String filename;
    private String message;
    private String fileId;
    private String hash;
    private long fileSize;
    private String fileType;
    private boolean isDuplicate;
    private String error;
    private FileModel fileModel;

    // Constructors
    public FileUploadResult() {}

    public FileUploadResult(boolean success, String filename, String message) {
        this.success = success;
        this.filename = filename;
        this.message = message;
    }

    // Static factory methods
    public static FileUploadResult success(String filename, FileModel fileModel, boolean isDuplicate) {
        FileUploadResult result = new FileUploadResult(true, filename, 
            isDuplicate ? "File uploaded (duplicate detected)" : "File uploaded successfully");
        result.setFileModel(fileModel);
        result.setFileId(fileModel.getId());
        result.setHash(fileModel.getHash());
        result.setFileSize(fileModel.getFileSize());
        result.setFileType(fileModel.getFileType());
        result.setDuplicate(isDuplicate);
        return result;
    }

    public static FileUploadResult error(String filename, String error) {
        FileUploadResult result = new FileUploadResult(false, filename, "Upload failed");
        result.setError(error);
        return result;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public boolean isDuplicate() {
        return isDuplicate;
    }

    public void setDuplicate(boolean duplicate) {
        isDuplicate = duplicate;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public FileModel getFileModel() {
        return fileModel;
    }

    public void setFileModel(FileModel fileModel) {
        this.fileModel = fileModel;
    }
}
