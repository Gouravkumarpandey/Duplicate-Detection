package com.yourname.filededup.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling GridFS file storage operations in MongoDB
 */
@Service
public class GridFSStorageService {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations gridFsOperations;

    /**
     * Store file in MongoDB GridFS
     */
    public GridFSFileInfo storeFileInGridFS(MultipartFile file, String uploadedBy) throws IOException {
        // Generate unique filename
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename(), uploadedBy);
        
        // Create metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalFileName", file.getOriginalFilename());
        metadata.put("uploadedBy", uploadedBy);
        metadata.put("uploadedDate", LocalDateTime.now().toString());
        metadata.put("contentType", file.getContentType());
        metadata.put("fileSize", file.getSize());
        
        // Store file in GridFS
        try (InputStream inputStream = file.getInputStream()) {
            Object fileId = gridFsTemplate.store(
                inputStream, 
                uniqueFileName, 
                file.getContentType(),
                metadata
            );
            
            return new GridFSFileInfo(
                fileId.toString(),
                uniqueFileName,
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                uploadedBy
            );
        }
    }

    /**
     * Retrieve file from GridFS
     */
    public GridFSFile getFile(String filename) {
        Query query = new Query(Criteria.where("filename").is(filename));
        return gridFsTemplate.findOne(query);
    }

    /**
     * Retrieve file by ID from GridFS
     */
    public GridFSFile getFileById(String fileId) {
        Query query = new Query(Criteria.where("_id").is(fileId));
        return gridFsTemplate.findOne(query);
    }

    /**
     * Delete file from GridFS
     */
    public void deleteFile(String filename) {
        Query query = new Query(Criteria.where("filename").is(filename));
        gridFsTemplate.delete(query);
    }

    /**
     * Delete file by ID from GridFS
     */
    public void deleteFileById(String fileId) {
        Query query = new Query(Criteria.where("_id").is(fileId));
        gridFsTemplate.delete(query);
    }

    /**
     * Check if file exists in GridFS
     */
    public boolean fileExists(String filename) {
        GridFSFile file = getFile(filename);
        return file != null;
    }

    /**
     * Get file content as InputStream
     */
    public InputStream getFileContent(String filename) throws IOException {
        GridFSFile file = getFile(filename);
        if (file == null) {
            throw new IOException("File not found: " + filename);
        }
        return gridFsOperations.getResource(file).getInputStream();
    }

    /**
     * Generate unique filename for GridFS storage
     */
    private String generateUniqueFileName(String originalFileName, String uploadedBy) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String sanitizedUser = sanitizeUserPath(uploadedBy);
        
        // Extract file extension
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        return "gridfs_" + sanitizedUser + "_" + timestamp + "_" + uuid + extension;
    }

    /**
     * Sanitize user path for safe filename creation
     */
    private String sanitizeUserPath(String user) {
        if (user == null) {
            return "anonymous";
        }
        return user.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    // ============ RESULT CLASS FOR GRIDFS OPERATIONS ============

    /**
     * Information about a file stored in GridFS
     */
    public static class GridFSFileInfo {
        private String fileId;
        private String gridFSFileName;
        private String originalFileName;
        private long fileSize;
        private String contentType;
        private String uploadedBy;

        public GridFSFileInfo(String fileId, String gridFSFileName, String originalFileName, 
                             long fileSize, String contentType, String uploadedBy) {
            this.fileId = fileId;
            this.gridFSFileName = gridFSFileName;
            this.originalFileName = originalFileName;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.uploadedBy = uploadedBy;
        }

        // Getters and setters
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        
        public String getGridFSFileName() { return gridFSFileName; }
        public void setGridFSFileName(String gridFSFileName) { this.gridFSFileName = gridFSFileName; }
        
        public String getOriginalFileName() { return originalFileName; }
        public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
        
        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    }
}
