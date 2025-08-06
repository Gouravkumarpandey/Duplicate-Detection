package com.yourname.filededup.repository;

import com.yourname.filededup.model.FileRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends MongoRepository<FileRecord, String> {
    
    // ============ BASIC FINDERS ============
    
    List<FileRecord> findByFileHash(String fileHash);
    
    List<FileRecord> findByCategory(String category);
    
    List<FileRecord> findByIsDuplicate(boolean isDuplicate);
    
    List<FileRecord> findByFileExtension(String fileExtension);
    
    List<FileRecord> findByFileName(String fileName);
    
    List<FileRecord> findByFilePath(String filePath);
    
    List<FileRecord> findByUploadedBy(String uploadedBy);
    
    List<FileRecord> findByMimeType(String mimeType);
    
    List<FileRecord> findByDuplicateGroupId(String duplicateGroupId);
    
    // ============ PAGINATED FINDERS ============
    
    Page<FileRecord> findByCategory(String category, Pageable pageable);
    
    Page<FileRecord> findByIsDuplicate(boolean isDuplicate, Pageable pageable);
    
    Page<FileRecord> findByUploadedBy(String uploadedBy, Pageable pageable);
    
    Page<FileRecord> findByFileExtension(String fileExtension, Pageable pageable);
    
    Page<FileRecord> findByDuplicateGroupId(String duplicateGroupId, Pageable pageable);
    
    // ============ SIZE-BASED QUERIES ============
    
    @Query("{ 'fileSize': { $gte: ?0, $lte: ?1 } }")
    List<FileRecord> findByFileSizeBetween(long minSize, long maxSize);
    
    @Query("{ 'fileSize': { $gte: ?0, $lte: ?1 } }")
    Page<FileRecord> findByFileSizeBetween(long minSize, long maxSize, Pageable pageable);
    
    @Query("{ 'fileSize': { $gte: ?0 } }")
    List<FileRecord> findByFileSizeGreaterThanEqual(long minSize);
    
    @Query("{ 'fileSize': { $lte: ?0 } }")
    List<FileRecord> findByFileSizeLessThanEqual(long maxSize);
    
    // ============ DATE-BASED QUERIES ============
    
    List<FileRecord> findByUploadedDateAfter(LocalDateTime date);
    
    List<FileRecord> findByUploadedDateBefore(LocalDateTime date);
    
    List<FileRecord> findByUploadedDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    List<FileRecord> findByScannedDateAfter(LocalDateTime date);
    
    List<FileRecord> findByLastVerifiedDateAfter(LocalDateTime date);
    
    Page<FileRecord> findByUploadedDateAfter(LocalDateTime date, Pageable pageable);
    
    Page<FileRecord> findByUploadedDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // ============ TEXT SEARCH QUERIES ============
    
    @Query("{ 'fileName': { $regex: ?0, $options: 'i' } }")
    List<FileRecord> findByFileNameContainingIgnoreCase(String fileName);
    
    @Query("{ 'fileName': { $regex: ?0, $options: 'i' } }")
    Page<FileRecord> findByFileNameContainingIgnoreCase(String fileName, Pageable pageable);
    
    @Query("{ 'description': { $regex: ?0, $options: 'i' } }")
    List<FileRecord> findByDescriptionContainingIgnoreCase(String description);
    
    @Query("{ 'tags': { $regex: ?0, $options: 'i' } }")
    List<FileRecord> findByTagsContainingIgnoreCase(String tags);
    
    @Query("{ 'tags': { $regex: ?0, $options: 'i' } }")
    Page<FileRecord> findByTagsContainingIgnoreCase(String tags, Pageable pageable);
    
    @Query("{ 'uploadedBy': { $regex: ?0, $options: 'i' } }")
    List<FileRecord> findByUploadedByContainingIgnoreCase(String uploadedBy);
    
    @Query("{ 'uploadedBy': { $regex: ?0, $options: 'i' } }")
    Page<FileRecord> findByUploadedByContainingIgnoreCase(String uploadedBy, Pageable pageable);
    
    // ============ COMBINED FILTER QUERIES ============
    
    @Query("{ 'category': ?0, 'isDuplicate': ?1 }")
    List<FileRecord> findByCategoryAndIsDuplicate(String category, boolean isDuplicate);
    
    @Query("{ 'category': ?0, 'isDuplicate': ?1 }")
    Page<FileRecord> findByCategoryAndIsDuplicate(String category, boolean isDuplicate, Pageable pageable);
    
    @Query("{ 'uploadedBy': ?0, 'isDuplicate': ?1 }")
    List<FileRecord> findByUploadedByAndIsDuplicate(String uploadedBy, boolean isDuplicate);
    
    @Query("{ 'uploadedBy': ?0, 'isDuplicate': ?1 }")
    Page<FileRecord> findByUploadedByAndIsDuplicate(String uploadedBy, boolean isDuplicate, Pageable pageable);
    
    @Query("{ 'category': ?0, 'uploadedBy': ?1 }")
    List<FileRecord> findByCategoryAndUploadedBy(String category, String uploadedBy);
    
    @Query("{ 'category': ?0, 'uploadedBy': ?1 }")
    Page<FileRecord> findByCategoryAndUploadedBy(String category, String uploadedBy, Pageable pageable);
    
    @Query("{ 'fileExtension': ?0, 'isDuplicate': ?1 }")
    List<FileRecord> findByFileExtensionAndIsDuplicate(String fileExtension, boolean isDuplicate);
    
    // ============ ADVANCED FILTER QUERIES ============
    
    @Query("{ 'category': ?0, 'fileSize': { $gte: ?1, $lte: ?2 } }")
    List<FileRecord> findByCategoryAndFileSizeBetween(String category, long minSize, long maxSize);
    
    @Query("{ 'uploadedBy': ?0, 'fileSize': { $gte: ?1, $lte: ?2 } }")
    List<FileRecord> findByUploadedByAndFileSizeBetween(String uploadedBy, long minSize, long maxSize);
    
    @Query("{ 'isDuplicate': ?0, 'fileSize': { $gte: ?1, $lte: ?2 } }")
    List<FileRecord> findByIsDuplicateAndFileSizeBetween(boolean isDuplicate, long minSize, long maxSize);
    
    @Query("{ 'uploadedDate': { $gte: ?0, $lte: ?1 }, 'isDuplicate': ?2 }")
    List<FileRecord> findByUploadedDateBetweenAndIsDuplicate(LocalDateTime startDate, LocalDateTime endDate, boolean isDuplicate);
    
    @Query("{ 'category': ?0, 'uploadedDate': { $gte: ?1, $lte: ?2 } }")
    List<FileRecord> findByCategoryAndUploadedDateBetween(String category, LocalDateTime startDate, LocalDateTime endDate);
    
    // ============ COMPLEX FILTER QUERIES ============
    
    @Query("{ $and: [ " +
           "{ $or: [ { 'fileName': { $regex: ?0, $options: 'i' } }, { 'fileName': { $exists: false } } ] }, " +
           "{ $or: [ { 'category': ?1 }, { 'category': { $exists: false } } ] }, " +
           "{ $or: [ { 'fileSize': { $gte: ?2 } }, { 'fileSize': { $exists: false } } ] }, " +
           "{ $or: [ { 'fileSize': { $lte: ?3 } }, { 'fileSize': { $exists: false } } ] }, " +
           "{ $or: [ { 'uploadedBy': { $regex: ?4, $options: 'i' } }, { 'uploadedBy': { $exists: false } } ] }, " +
           "{ $or: [ { 'tags': { $regex: ?5, $options: 'i' } }, { 'tags': { $exists: false } } ] } " +
           "] }")
    List<FileRecord> findByAdvancedFilters(String fileName, String category, Long minSize, Long maxSize, String uploadedBy, String tags);
    
    @Query("{ $and: [ " +
           "{ $or: [ { 'fileName': { $regex: ?0, $options: 'i' } }, { 'fileName': { $exists: false } } ] }, " +
           "{ $or: [ { 'category': ?1 }, { 'category': { $exists: false } } ] }, " +
           "{ $or: [ { 'fileSize': { $gte: ?2 } }, { 'fileSize': { $exists: false } } ] }, " +
           "{ $or: [ { 'fileSize': { $lte: ?3 } }, { 'fileSize': { $exists: false } } ] }, " +
           "{ $or: [ { 'uploadedBy': { $regex: ?4, $options: 'i' } }, { 'uploadedBy': { $exists: false } } ] }, " +
           "{ $or: [ { 'tags': { $regex: ?5, $options: 'i' } }, { 'tags': { $exists: false } } ] } " +
           "] }")
    Page<FileRecord> findByAdvancedFilters(String fileName, String category, Long minSize, Long maxSize, String uploadedBy, String tags, Pageable pageable);
    
    // ============ HASH-BASED QUERIES ============
    
    @Query("{ 'fileHash': { $in: ?0 } }")
    List<FileRecord> findByFileHashIn(List<String> hashes);
    
    @Query("{ 'fileHash': { $in: ?0 } }")
    Page<FileRecord> findByFileHashIn(List<String> hashes, Pageable pageable);
    
    @Query("{ 'checksum': ?0 }")
    List<FileRecord> findByChecksum(String checksum);
    
    @Query("{ 'checksum': { $in: ?0 } }")
    List<FileRecord> findByChecksumIn(List<String> checksums);
    
    // ============ VERIFICATION QUERIES ============
    
    List<FileRecord> findByIsVerified(boolean isVerified);
    
    Page<FileRecord> findByIsVerified(boolean isVerified, Pageable pageable);
    
    @Query("{ 'lastVerifiedDate': { $exists: false } }")
    List<FileRecord> findUnverifiedFiles();
    
    @Query("{ 'lastVerifiedDate': { $exists: false } }")
    Page<FileRecord> findUnverifiedFiles(Pageable pageable);
    
    @Query("{ 'lastVerifiedDate': { $lt: ?0 } }")
    List<FileRecord> findFilesVerifiedBefore(LocalDateTime date);
    
    // ============ EXISTENCE CHECKS ============
    
    boolean existsByFileHash(String fileHash);
    
    boolean existsByFilePath(String filePath);
    
    boolean existsByFileName(String fileName);
    
    boolean existsByFileNameAndUploadedBy(String fileName, String uploadedBy);
    
    boolean existsByChecksumAndUploadedBy(String checksum, String uploadedBy);
    
    boolean existsByDuplicateGroupId(String duplicateGroupId);
    
    // ============ COUNT QUERIES ============
    
    long countByCategory(String category);
    
    long countByIsDuplicate(boolean isDuplicate);
    
    long countByUploadedBy(String uploadedBy);
    
    long countByFileExtension(String fileExtension);
    
    long countByMimeType(String mimeType);
    
    long countByDuplicateGroupId(String duplicateGroupId);
    
    @Query(value = "{ 'fileSize': { $gte: ?0, $lte: ?1 } }", count = true)
    long countByFileSizeBetween(long minSize, long maxSize);
    
    @Query(value = "{ 'uploadedDate': { $gte: ?0, $lte: ?1 } }", count = true)
    long countByUploadedDateBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    @Query(value = "{ 'category': ?0, 'isDuplicate': ?1 }", count = true)
    long countByCategoryAndIsDuplicate(String category, boolean isDuplicate);
    
    @Query(value = "{ 'uploadedBy': ?0, 'isDuplicate': ?1 }", count = true)
    long countByUploadedByAndIsDuplicate(String uploadedBy, boolean isDuplicate);
    
    // ============ AGGREGATION QUERIES ============
    
    @Aggregation(pipeline = {
        "{ $group: { _id: '$category', count: { $sum: 1 }, totalSize: { $sum: '$fileSize' } } }",
        "{ $sort: { count: -1 } }"
    })
    List<CategoryStatistics> getCategoryStatistics();
    
    @Aggregation(pipeline = {
        "{ $group: { _id: '$uploadedBy', count: { $sum: 1 }, totalSize: { $sum: '$fileSize' }, duplicates: { $sum: { $cond: ['$isDuplicate', 1, 0] } } } }",
        "{ $sort: { count: -1 } }"
    })
    List<UserStatistics> getUserStatistics();
    
    @Aggregation(pipeline = {
        "{ $group: { _id: '$fileExtension', count: { $sum: 1 }, totalSize: { $sum: '$fileSize' } } }",
        "{ $sort: { count: -1 } }"
    })
    List<ExtensionStatistics> getExtensionStatistics();
    
    @Aggregation(pipeline = {
        "{ $group: { _id: '$duplicateGroupId', count: { $sum: 1 }, files: { $push: '$$ROOT' } } }",
        "{ $match: { count: { $gt: 1 } } }",
        "{ $sort: { count: -1 } }"
    })
    List<DuplicateGroupInfo> getDuplicateGroupsInfo();
    
    @Aggregation(pipeline = {
        "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$uploadedDate' } }, count: { $sum: 1 }, totalSize: { $sum: '$fileSize' } } }",
        "{ $sort: { _id: -1 } }",
        "{ $limit: ?0 }"
    })
    List<DailyUploadStatistics> getDailyUploadStatistics(int days);
    
    @Aggregation(pipeline = {
        "{ $bucket: { " +
        "    groupBy: '$fileSize', " +
        "    boundaries: [0, 1024, 1048576, 104857600, 1073741824, Double.POSITIVE_INFINITY], " +
        "    default: 'Other', " +
        "    output: { count: { $sum: 1 }, totalSize: { $sum: '$fileSize' } } " +
        "} }"
    })
    List<SizeDistributionStatistics> getSizeDistributionStatistics();
    
    // ============ CUSTOM FIND METHODS ============
    
    @Query("{ 'storageLocation': { $exists: true, $ne: null } }")
    List<FileRecord> findFilesWithStorageLocation();
    
    @Query("{ 'storageLocation': { $exists: false } }")
    List<FileRecord> findFilesWithoutStorageLocation();
    
    @Query("{ 'duplicateGroupId': { $exists: true, $ne: null } }")
    List<FileRecord> findAllDuplicateGroupFiles();
    
    @Query("{ 'duplicateGroupId': { $exists: true, $ne: null } }")
    Page<FileRecord> findAllDuplicateGroupFiles(Pageable pageable);
    
    @Query("{ 'tags': { $exists: true, $ne: null, $ne: '' } }")
    List<FileRecord> findFilesWithTags();
    
    @Query("{ 'description': { $exists: true, $ne: null, $ne: '' } }")
    List<FileRecord> findFilesWithDescription();
    
    // ============ RECENT ACTIVITY QUERIES ============
    
    @Query("{ 'uploadedDate': { $gte: ?0 } }")
    List<FileRecord> findRecentlyUploadedFiles(LocalDateTime since);
    
    @Query("{ 'uploadedDate': { $gte: ?0 } }")
    Page<FileRecord> findRecentlyUploadedFiles(LocalDateTime since, Pageable pageable);
    
    @Query("{ 'lastAccessDate': { $gte: ?0 } }")
    List<FileRecord> findRecentlyAccessedFiles(LocalDateTime since);
    
    @Query("{ 'lastAccessDate': { $gte: ?0 } }")
    Page<FileRecord> findRecentlyAccessedFiles(LocalDateTime since, Pageable pageable);
    
    @Query("{ 'scannedDate': { $gte: ?0 } }")
    List<FileRecord> findRecentlyScannedFiles(LocalDateTime since);
    
    // ============ ORPHANED AND CLEANUP QUERIES ============
    
    @Query("{ 'storageLocation': { $exists: true }, 'uploadedBy': { $exists: false } }")
    List<FileRecord> findOrphanedFiles();
    
    @Query("{ 'duplicateGroupId': { $exists: true }, 'isDuplicate': false }")
    List<FileRecord> findInconsistentDuplicateStatus();
    
    @Query("{ 'fileHash': { $exists: false } }")
    List<FileRecord> findFilesWithoutHash();
    
    @Query("{ 'checksum': { $exists: false } }")
    List<FileRecord> findFilesWithoutChecksum();
    
    // ============ PERFORMANCE OPTIMIZATION QUERIES ============
    
    @Query(value = "{ 'isDuplicate': true }", fields = "{ 'id': 1, 'fileName': 1, 'fileSize': 1, 'duplicateGroupId': 1 }")
    List<FileRecord> findDuplicatesMinimal();
    
    @Query(value = "{ 'category': ?0 }", fields = "{ 'id': 1, 'fileName': 1, 'fileSize': 1 }")
    List<FileRecord> findByCategoryMinimal(String category);
    
    @Query(value = "{ 'uploadedBy': ?0 }", fields = "{ 'id': 1, 'fileName': 1, 'fileSize': 1, 'uploadedDate': 1 }")
    List<FileRecord> findByUploadedByMinimal(String uploadedBy);
    
    // ============ DELETE OPERATIONS ============
    
    @Query(value = "{ 'fileHash': ?0 }", delete = true)
    void deleteByFileHash(String fileHash);
    
    @Query(value = "{ 'duplicateGroupId': ?0 }", delete = true)
    void deleteByDuplicateGroupId(String duplicateGroupId);
    
    @Query(value = "{ 'uploadedBy': ?0 }", delete = true)
    void deleteByUploadedBy(String uploadedBy);
    
    @Query(value = "{ 'category': ?0 }", delete = true)
    void deleteByCategory(String category);
    
    @Query(value = "{ 'uploadedDate': { $lt: ?0 } }", delete = true)
    void deleteByUploadedDateBefore(LocalDateTime date);
    
    @Query(value = "{ 'isDuplicate': true, 'duplicateGroupId': { $exists: false } }", delete = true)
    void deleteOrphanedDuplicates();
    
    // ============ BULK OPERATIONS ============
    
    @Query("{ 'id': { $in: ?0 } }")
    List<FileRecord> findByIdIn(List<String> ids);
    
    @Query("{ 'fileName': { $in: ?0 } }")
    List<FileRecord> findByFileNameIn(List<String> fileNames);
    
    @Query("{ 'category': { $in: ?0 } }")
    List<FileRecord> findByCategoryIn(List<String> categories);
    
    @Query("{ 'uploadedBy': { $in: ?0 } }")
    List<FileRecord> findByUploadedByIn(List<String> uploaders);
    
    // ============ STATISTICAL HELPER INTERFACES ============
    
    public static interface CategoryStatistics {
        String getId();
        long getCount();
        long getTotalSize();
    }
    
    public static interface UserStatistics {
        String getId();
        long getCount();
        long getTotalSize();
        long getDuplicates();
    }
    
    public static interface ExtensionStatistics {
        String getId();
        long getCount();
        long getTotalSize();
    }
    
    public static interface DuplicateGroupInfo {
        String getId();
        long getCount();
        List<FileRecord> getFiles();
    }
    
    public static interface DailyUploadStatistics {
        String getId();
        long getCount();
        long getTotalSize();
    }
    
    public static interface SizeDistributionStatistics {
        String getId();
        long getCount();
        long getTotalSize();
    }