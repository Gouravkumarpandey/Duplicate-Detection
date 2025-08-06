package com.yourname.filededup.controller;

import com.yourname.filededup.model.LogEntry;
import com.yourname.filededup.service.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private LoggingService loggingService;

    @GetMapping
    public ResponseEntity<List<LogEntry>> getAllLogs() {
        List<LogEntry> logs = loggingService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<LogEntry>> getLogsByLevel(@PathVariable String level) {
        List<LogEntry> logs = loggingService.getLogsByLevel(level);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/recent/{count}")
    public ResponseEntity<List<LogEntry>> getRecentLogs(@PathVariable int count) {
        List<LogEntry> logs = loggingService.getRecentLogs(count);
        return ResponseEntity.ok(logs);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearLogs() {
        loggingService.clearLogs();
        return ResponseEntity.ok("Logs cleared successfully");
    }
}
