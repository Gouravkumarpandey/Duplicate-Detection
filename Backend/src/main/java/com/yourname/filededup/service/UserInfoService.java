package com.yourname.filededup.service;

import com.yourname.filededup.model.UserInfo;
import com.yourname.filededup.repository.UserInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for handling user information operations
 */
@Service
public class UserInfoService {

    @Autowired
    private UserInfoRepository userInfoRepository;

    /**
     * Save user information to MongoDB Atlas
     */
    public UserInfo saveUserInfo(UserInfo userInfo) {
        // Check if email already exists
        if (userInfoRepository.existsByEmail(userInfo.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userInfo.getEmail());
        }

        // Validate required fields
        if (userInfo.getName() == null || userInfo.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (userInfo.getEmail() == null || userInfo.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // Validate email format
        if (!isValidEmail(userInfo.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Save to MongoDB
        return userInfoRepository.save(userInfo);
    }

    /**
     * Get all users from MongoDB Atlas
     */
    public List<UserInfo> getAllUsers() {
        return userInfoRepository.findAll();
    }

    /**
     * Get user by ID
     */
    public Optional<UserInfo> getUserById(String id) {
        return userInfoRepository.findById(id);
    }

    /**
     * Get user by email
     */
    public Optional<UserInfo> getUserByEmail(String email) {
        return userInfoRepository.findByEmail(email);
    }

    /**
     * Update user information
     */
    public UserInfo updateUserInfo(String id, UserInfo updatedUserInfo) {
        Optional<UserInfo> existingUser = userInfoRepository.findById(id);
        
        if (existingUser.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + id);
        }

        UserInfo userToUpdate = existingUser.get();
        
        // Update fields if provided
        if (updatedUserInfo.getName() != null) {
            userToUpdate.setName(updatedUserInfo.getName());
        }
        if (updatedUserInfo.getPhone() != null) {
            userToUpdate.setPhone(updatedUserInfo.getPhone());
        }
        if (updatedUserInfo.getAddress() != null) {
            userToUpdate.setAddress(updatedUserInfo.getAddress());
        }
        if (updatedUserInfo.getCity() != null) {
            userToUpdate.setCity(updatedUserInfo.getCity());
        }
        if (updatedUserInfo.getCountry() != null) {
            userToUpdate.setCountry(updatedUserInfo.getCountry());
        }

        return userInfoRepository.save(userToUpdate);
    }

    /**
     * Delete user by ID
     */
    public boolean deleteUser(String id) {
        if (userInfoRepository.existsById(id)) {
            userInfoRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Search users by name
     */
    public List<UserInfo> searchUsersByName(String name) {
        return userInfoRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Get users by city
     */
    public List<UserInfo> getUsersByCity(String city) {
        return userInfoRepository.findByCity(city);
    }

    /**
     * Get users by country
     */
    public List<UserInfo> getUsersByCountry(String country) {
        return userInfoRepository.findByCountry(country);
    }

    /**
     * Get total user count
     */
    public long getTotalUserCount() {
        return userInfoRepository.count();
    }

    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(emailRegex);
    }
}
