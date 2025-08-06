package com.yourname.filededup.repository;

import com.yourname.filededup.model.FileRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends MongoRepository<FileRecord, String> {
    
    List<FileRecord> findByFileHash(String fileHash);
    
    List<FileRecord> findByCategory(String category);
    
    List<FileRecord> findByIsDuplicate(boolean isDuplicate);
    
    List<FileRecord> findByFileExtension(String fileExtension);
    
    List<FileRecord> findByFileName(String fileName);
    
    List<FileRecord> findByFilePath(String filePath);
    
    @Query("{ 'fileSize': { $gte: ?0, $lte: ?1 } }")
    List<FileRecord> findByFileSizeBetween(long minSize, long maxSize);
    
    @Query("{ 'fileHash': { $in: ?0 } }")
    List<FileRecord> findByFileHashIn(List<String> hashes);
    
    boolean existsByFileHash(String fileHash);
    
    boolean existsByFilePath(String filePath);
    
    long countByCategory(String category);
    
    long countByIsDuplicate(boolean isDuplicate);
    
    @Query(value = "{ 'fileHash': ?0 }", delete = true)
    void deleteByFileHash(String fileHash);
}
