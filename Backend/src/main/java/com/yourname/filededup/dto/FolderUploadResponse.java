package com.yourname.filededup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for folder upload operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FolderUploadResponse {
    
    private boolean success;
    private String message;
    private int totalFiles;
    private int successfulUploads;
    private int failedUploads;
    private int duplicatesFound;
    private List<FileUploadResult> uploadResults;
    private List<String> errors;
    private Map<String, Object> statistics;

    // Constructors
    public FolderUploadResponse() {}

    public FolderUploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Static factory methods
    public static FolderUploadResponse success(String message, List<FileUploadResult> results) {
        FolderUploadResponse response = new FolderUploadResponse(true, message);
        response.setUploadResults(results);
        response.setTotalFiles(results.size());
        response.setSuccessfulUploads((int) results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum());
        response.setFailedUploads((int) results.stream().mapToLong(r -> !r.isSuccess() ? 1 : 0).sum());
        response.setDuplicatesFound((int) results.stream().mapToLong(r -> r.isDuplicate() ? 1 : 0).sum());
        return response;
    }

    public static FolderUploadResponse error(String message) {
        return new FolderUploadResponse(false, message);
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

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getSuccessfulUploads() {
        return successfulUploads;
    }

    public void setSuccessfulUploads(int successfulUploads) {
        this.successfulUploads = successfulUploads;
    }

    public int getFailedUploads() {
        return failedUploads;
    }

    public void setFailedUploads(int failedUploads) {
        this.failedUploads = failedUploads;
    }

    public int getDuplicatesFound() {
        return duplicatesFound;
    }

    public void setDuplicatesFound(int duplicatesFound) {
        this.duplicatesFound = duplicatesFound;
    }

    public List<FileUploadResult> getUploadResults() {
        return uploadResults;
    }

    public void setUploadResults(List<FileUploadResult> uploadResults) {
        this.uploadResults = uploadResults;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
}
