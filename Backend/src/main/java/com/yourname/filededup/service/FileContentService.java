package com.yourname.filededup.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FileContentService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileContentService.class);

    /**
     * Extracts text content from different file types
     */
    public String extractTextContent(MultipartFile file, String fileExtension) throws IOException {
        String content = "";
        
        switch (fileExtension.toLowerCase()) {
            case ".txt":
                content = extractTextFromTxt(file);
                break;
            case ".pdf":
                content = extractTextFromPdf(file);
                break;
            case ".docx":
                content = extractTextFromDocx(file);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileExtension);
        }
        
        return content;
    }

    /**
     * Extract text from .txt files using BufferedReader
     */
    private String extractTextFromTxt(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        
        logger.debug("Extracted {} characters from TXT file: {}", content.length(), file.getOriginalFilename());
        return content.toString();
    }

    /**
     * Extract text from .pdf files using Apache PDFBox
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            content.append(stripper.getText(document));
        } catch (Exception e) {
            logger.error("Error extracting text from PDF file: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
        
        logger.debug("Extracted {} characters from PDF file: {}", content.length(), file.getOriginalFilename());
        return content.toString();
    }

    /**
     * Extract text from .docx files using Apache POI
     */
    private String extractTextFromDocx(MultipartFile file) throws IOException {
        StringBuilder content = new StringBuilder();
        
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            
            content.append(extractor.getText());
        } catch (Exception e) {
            logger.error("Error extracting text from DOCX file: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to extract text from DOCX: " + e.getMessage(), e);
        }
        
        logger.debug("Extracted {} characters from DOCX file: {}", content.length(), file.getOriginalFilename());
        return content.toString();
    }

    /**
     * Calculate SHA-256 hash of file content
     */
    public String calculateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to calculate hash", e);
        }
    }

    /**
     * Calculate SHA-256 hash of the actual file bytes (for file integrity)
     */
    public String calculateFileHash(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            try (InputStream inputStream = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            
            byte[] hash = digest.digest();
            
            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }

    /**
     * Validate file type based on extension and content
     */
    public boolean isValidFileType(String fileName, String contentType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        
        // Check allowed extensions
        boolean validExtension = extension.equals("txt") || 
                               extension.equals("pdf") || 
                               extension.equals("docx");
        
        // Check MIME types (additional validation)
        boolean validMimeType = false;
        if (contentType != null) {
            validMimeType = contentType.equals("text/plain") ||
                           contentType.equals("application/pdf") ||
                           contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        
        return validExtension && (contentType == null || validMimeType);
    }

    /**
     * Get file extension from filename
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /**
     * Generate a unique filename to avoid conflicts
     */
    public String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        return baseName + "_" + timestamp + extension;
    }
}
