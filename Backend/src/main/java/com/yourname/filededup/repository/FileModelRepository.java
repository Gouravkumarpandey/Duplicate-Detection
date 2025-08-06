package com.yourname.filededup.repository;

import com.yourname.filededup.model.FileModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for FileModel operations
 */
@Repository
public interface FileModelRepository extends MongoRepository<FileModel, String> {
    
    /**
     * Find file by SHA-256 hash to check for duplicates
     * @param hash SHA-256 hash of the file
     * @return Optional FileModel if exists
     */
    Optional<FileModel> findByHash(String hash);
    
    /**
     * Check if a file with given hash exists
     * @param hash SHA-256 hash of the file
     * @return true if file exists, false otherwise
     */
    boolean existsByHash(String hash);
    
    /**
     * Find all duplicate files (files with isDuplicate = true)
     * @return List of duplicate FileModel objects
     */
    List<FileModel> findByIsDuplicateTrue();
    
    /**
     * Find all unique files (files with isDuplicate = false)
     * @return List of unique FileModel objects
     */
    List<FileModel> findByIsDuplicateFalse();
    
    /**
     * Find files by file type
     * @param fileType File extension (e.g., .txt, .pdf, .docx)
     * @return List of FileModel objects with matching file type
     */
    List<FileModel> findByFileType(String fileType);
    
    /**
     * Find files uploaded after a specific date
     * @param uploadDate The date to filter from
     * @return List of FileModel objects uploaded after the date
     */
    List<FileModel> findByUploadDateAfter(LocalDateTime uploadDate);
    
    /**
     * Find files by filename (partial match, case-insensitive)
     * @param filename Filename to search for
     * @return List of FileModel objects with matching filename
     */
    @Query("{'filename': {$regex: ?0, $options: 'i'}}")
    List<FileModel> findByFilenameContainingIgnoreCase(String filename);
    
    /**
     * Count total number of duplicate files
     * @return Number of files marked as duplicates
     */
    long countByIsDuplicateTrue();
    
    /**
     * Count total number of unique files
     * @return Number of files marked as unique
     */
    long countByIsDuplicateFalse();
    
    /**
     * Find files with size greater than specified bytes
     * @param sizeInBytes File size threshold
     * @return List of FileModel objects larger than specified size
     */
    List<FileModel> findByFileSizeGreaterThan(Long sizeInBytes);
    
    /**
     * Find files uploaded within a date range
     * @param startDate Start date
     * @param endDate End date
     * @return List of FileModel objects uploaded within the range
     */
    List<FileModel> findByUploadDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Delete files older than specified date
     * @param cutoffDate Date before which files should be deleted
     * @return Number of deleted files
     */
    long deleteByUploadDateBefore(LocalDateTime cutoffDate);
    
    /**
     * Find all files with same hash (all duplicates of a file)
     * @param hash SHA-256 hash
     * @return List of all files with the same hash
     */
    List<FileModel> findAllByHash(String hash);
}
