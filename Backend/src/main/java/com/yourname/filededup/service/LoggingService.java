package com.yourname.filededup.service;

import com.yourname.filededup.model.LogEntry;
import com.yourname.filededup.repository.LogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoggingService {

    @Autowired
    private LogRepository logRepository;

    public void logInfo(String message, String operation, String details) {
        LogEntry logEntry = new LogEntry(LogEntry.Level.INFO, message, operation, details);
        logRepository.save(logEntry);
    }

    public void logWarn(String message, String operation, String details) {
        LogEntry logEntry = new LogEntry(LogEntry.Level.WARN, message, operation, details);
        logRepository.save(logEntry);
    }

    public void logError(String message, String operation, String details) {
        LogEntry logEntry = new LogEntry(LogEntry.Level.ERROR, message, operation, details);
        logRepository.save(logEntry);
    }

    public void logDebug(String message, String operation, String details) {
        LogEntry logEntry = new LogEntry(LogEntry.Level.DEBUG, message, operation, details);
        logRepository.save(logEntry);
    }

    public List<LogEntry> getAllLogs() {
        return logRepository.findAll();
    }

    public List<LogEntry> getLogsByLevel(String level) {
        return logRepository.findByLevel(level);
    }

    public List<LogEntry> getRecentLogs(int count) {
        return logRepository.findAllOrderByTimestampDesc(PageRequest.of(0, count));
    }

    public List<LogEntry> getLogsByOperation(String operation) {
        return logRepository.findByOperation(operation);
    }

    public List<LogEntry> getLogsBetween(LocalDateTime start, LocalDateTime end) {
        return logRepository.findByTimestampBetween(start, end);
    }

    public List<LogEntry> searchLogs(String keyword) {
        return logRepository.findByMessageContainingIgnoreCase(keyword);
    }

    public void clearLogs() {
        logRepository.deleteAll();
        logInfo("All logs cleared", "ADMIN", "Log database cleared by user request");
    }

    public void clearOldLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        logRepository.deleteByTimestampBefore(cutoffDate);
        logInfo("Old logs cleared", "ADMIN", 
            "Logs older than " + daysToKeep + " days have been removed");
    }

    public long getLogCountByLevel(String level) {
        return logRepository.countByLevel(level);
    }
}
