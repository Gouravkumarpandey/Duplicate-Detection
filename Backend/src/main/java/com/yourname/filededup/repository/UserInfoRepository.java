package com.yourname.filededup.repository;

import com.yourname.filededup.model.UserInfo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository interface for UserInfo operations with MongoDB
 */
@Repository
public interface UserInfoRepository extends MongoRepository<UserInfo, String> {
    
    /**
     * Find user by email address
     */
    Optional<UserInfo> findByEmail(String email);
    
    /**
     * Check if user exists by email
     */
    boolean existsByEmail(String email);
    
    /**
     * Find users by name (case-insensitive)
     */
    List<UserInfo> findByNameContainingIgnoreCase(String name);
    
    /**
     * Find users by city
     */
    List<UserInfo> findByCity(String city);
    
    /**
     * Find users by country
     */
    List<UserInfo> findByCountry(String country);
    
    /**
     * Count total users
     */
    long count();
}
