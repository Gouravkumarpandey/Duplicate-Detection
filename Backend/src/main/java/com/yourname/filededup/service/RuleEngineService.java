package com.yourname.filededup.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourname.filededup.model.FileRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class RuleEngineService {

    @Autowired
    private LoggingService loggingService;

    private Map<String, String> categoryRules;
    private Map<String, String> extensionToCategory;

    @PostConstruct
    public void initializeRules() {
        loadCategorizationRules();
    }

    public String categorizeFile(FileRecord fileRecord) {
        String category = categorizeByExtension(fileRecord.getFileExtension());
        
        if ("Other".equals(category)) {
            category = categorizeByMimeType(fileRecord.getMimeType());
        }
        
        if ("Other".equals(category)) {
            category = categorizeBySize(fileRecord.getFileSize());
        }
        
        loggingService.logInfo("File categorized", "CATEGORIZE", 
            "File: " + fileRecord.getFileName() + ", Category: " + category);
        
        return category;
    }

    private String categorizeByExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "Other";
        }
        
        return extensionToCategory.getOrDefault(extension.toLowerCase(), "Other");
    }

    private String categorizeByMimeType(String mimeType) {
        if (mimeType == null) {
            return "Other";
        }
        
        String type = mimeType.toLowerCase();
        
        if (type.startsWith("image/")) {
            return "Images";
        } else if (type.startsWith("video/")) {
            return "Videos";
        } else if (type.startsWith("audio/")) {
            return "Audio";
        } else if (type.startsWith("text/") || type.contains("document")) {
            return "Documents";
        } else if (type.contains("application/zip") || type.contains("compressed")) {
            return "Archives";
        }
        
        return "Other";
    }

    private String categorizeBySize(long fileSize) {
        // Size-based categorization as fallback
        if (fileSize > 100 * 1024 * 1024) { // > 100MB
            return "Large Files";
        } else if (fileSize < 1024) { // < 1KB
            return "Small Files";
        }
        
        return "Other";
    }

    private void loadCategorizationRules() {
        try {
            ClassPathResource resource = new ClassPathResource("rules/categorization-rules.json");
            InputStream inputStream = resource.getInputStream();
            
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(inputStream);
            
            categoryRules = new HashMap<>();
            extensionToCategory = new HashMap<>();
            
            JsonNode categoriesNode = rootNode.get("categories");
            if (categoriesNode != null) {
                Iterator<Map.Entry<String, JsonNode>> categories = categoriesNode.fields();
                while (categories.hasNext()) {
                    Map.Entry<String, JsonNode> categoryEntry = categories.next();
                    String categoryName = categoryEntry.getKey();
                    JsonNode extensionsNode = categoryEntry.getValue().get("extensions");
                    
                    if (extensionsNode != null && extensionsNode.isArray()) {
                        for (JsonNode extensionNode : extensionsNode) {
                            String extension = extensionNode.asText().toLowerCase();
                            extensionToCategory.put(extension, categoryName);
                        }
                    }
                }
            }
            
            loggingService.logInfo("Categorization rules loaded", "INIT", 
                "Loaded " + extensionToCategory.size() + " extension mappings");
            
        } catch (IOException e) {
            loggingService.logError("Failed to load categorization rules", "INIT", 
                "Error: " + e.getMessage());
            initializeDefaultRules();
        }
    }

    private void initializeDefaultRules() {
        extensionToCategory = new HashMap<>();
        
        // Images
        String[] imageExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "tiff", "svg", "webp"};
        for (String ext : imageExtensions) {
            extensionToCategory.put(ext, "Images");
        }
        
        // Videos
        String[] videoExtensions = {"mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v"};
        for (String ext : videoExtensions) {
            extensionToCategory.put(ext, "Videos");
        }
        
        // Audio
        String[] audioExtensions = {"mp3", "wav", "flac", "aac", "ogg", "wma", "m4a"};
        for (String ext : audioExtensions) {
            extensionToCategory.put(ext, "Audio");
        }
        
        // Documents
        String[] documentExtensions = {"pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx"};
        for (String ext : documentExtensions) {
            extensionToCategory.put(ext, "Documents");
        }
        
        // Archives
        String[] archiveExtensions = {"zip", "rar", "7z", "tar", "gz", "bz2", "xz"};
        for (String ext : archiveExtensions) {
            extensionToCategory.put(ext, "Archives");
        }
        
        // Code
        String[] codeExtensions = {"java", "js", "ts", "py", "cpp", "c", "h", "css", "html", "xml", "json"};
        for (String ext : codeExtensions) {
            extensionToCategory.put(ext, "Code");
        }
        
        loggingService.logInfo("Default categorization rules initialized", "INIT", 
            "Loaded " + extensionToCategory.size() + " default extension mappings");
    }

    public Map<String, String> getAllCategoryRules() {
        return new HashMap<>(extensionToCategory);
    }

    public void updateCategoryRule(String extension, String category) {
        extensionToCategory.put(extension.toLowerCase(), category);
        loggingService.logInfo("Category rule updated", "UPDATE", 
            "Extension: " + extension + ", Category: " + category);
    }
}
