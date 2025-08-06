package com.yourname.filededup.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Document(collection = "log_entries")
public class LogEntry {
    
    @Id
    private String id;
    
    @Indexed
    private String level;
    
    private String message;
    
    private String operation;
    
    private String details;
    
    @Indexed
    private LocalDateTime timestamp;
    
    private String userId;
    
    private String sessionId;

    // Default constructor
    public LogEntry() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with essential fields
    public LogEntry(String level, String message, String operation) {
        this();
        this.level = level;
        this.message = message;
        this.operation = operation;
    }

    // Constructor with all fields
    public LogEntry(String level, String message, String operation, String details) {
        this(level, message, operation);
        this.details = details;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    // Log levels constants
    public static class Level {
        public static final String INFO = "INFO";
        public static final String WARN = "WARN";
        public static final String ERROR = "ERROR";
        public static final String DEBUG = "DEBUG";
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id='" + id + '\'' +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", operation='" + operation + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
