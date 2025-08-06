package com.yourname.filededup.repository;

import com.yourname.filededup.model.LogEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends MongoRepository<LogEntry, String> {
    
    List<LogEntry> findByLevel(String level);
    
    List<LogEntry> findByOperation(String operation);
    
    @Query("{ 'timestamp': { $gte: ?0, $lte: ?1 } }")
    List<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'level': ?0, 'timestamp': { $gte: ?1 } }")
    List<LogEntry> findByLevelAndTimestampAfter(String level, LocalDateTime timestamp);
    
    List<LogEntry> findByUserId(String userId);
    
    List<LogEntry> findBySessionId(String sessionId);
    
    @Query(value = "{}", sort = "{ 'timestamp': -1 }")
    List<LogEntry> findAllOrderByTimestampDesc(Pageable pageable);
    
    @Query("{ 'message': { $regex: ?0, $options: 'i' } }")
    List<LogEntry> findByMessageContainingIgnoreCase(String keyword);
    
    long countByLevel(String level);
    
    @Query(value = "{ 'timestamp': { $lt: ?0 } }", delete = true)
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
