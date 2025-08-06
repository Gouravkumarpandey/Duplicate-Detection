package com.yourname.filededup.controller;

import com.yourname.filededup.dto.UserInfoResponse;
import com.yourname.filededup.model.UserInfo;
import com.yourname.filededup.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for handling user information operations
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5175", "http://localhost:5176"})
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * Save user information to MongoDB Atlas
     * POST /api/user-info
     */
    @PostMapping("/user-info")
    public ResponseEntity<UserInfoResponse> saveUserInfo(@RequestBody UserInfo userInfo) {
        try {
            UserInfo savedUser = userInfoService.saveUserInfo(userInfo);
            UserInfoResponse response = UserInfoResponse.success(
                "User information saved successfully to MongoDB Atlas", 
                savedUser
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(UserInfoResponse.error("Validation error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(UserInfoResponse.error("Failed to save user information: " + e.getMessage()));
        }
    }

    /**
     * Get all users from MongoDB Atlas
     * GET /api/user-info/all
     */
    @GetMapping("/user-info/all")
    public ResponseEntity<List<UserInfo>> getAllUsers() {
        try {
            List<UserInfo> users = userInfoService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user by ID
     * GET /api/user-info/{id}
     */
    @GetMapping("/user-info/{id}")
    public ResponseEntity<UserInfo> getUserById(@PathVariable String id) {
        try {
            Optional<UserInfo> user = userInfoService.getUserById(id);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user by email
     * GET /api/user-info/email/{email}
     */
    @GetMapping("/user-info/email/{email}")
    public ResponseEntity<UserInfo> getUserByEmail(@PathVariable String email) {
        try {
            Optional<UserInfo> user = userInfoService.getUserByEmail(email);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update user information
     * PUT /api/user-info/{id}
     */
    @PutMapping("/user-info/{id}")
    public ResponseEntity<UserInfoResponse> updateUserInfo(
            @PathVariable String id, 
            @RequestBody UserInfo userInfo) {
        try {
            UserInfo updatedUser = userInfoService.updateUserInfo(id, userInfo);
            UserInfoResponse response = UserInfoResponse.success(
                "User information updated successfully", 
                updatedUser
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(UserInfoResponse.error("Validation error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(UserInfoResponse.error("Failed to update user information: " + e.getMessage()));
        }
    }

    /**
     * Delete user by ID
     * DELETE /api/user-info/{id}
     */
    @DeleteMapping("/user-info/{id}")
    public ResponseEntity<UserInfoResponse> deleteUser(@PathVariable String id) {
        try {
            boolean deleted = userInfoService.deleteUser(id);
            if (deleted) {
                return ResponseEntity.ok(
                    UserInfoResponse.success("User deleted successfully", null)
                );
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(UserInfoResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    /**
     * Search users by name
     * GET /api/user-info/search?name={name}
     */
    @GetMapping("/user-info/search")
    public ResponseEntity<List<UserInfo>> searchUsers(@RequestParam String name) {
        try {
            List<UserInfo> users = userInfoService.searchUsersByName(name);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get users by city
     * GET /api/user-info/city/{city}
     */
    @GetMapping("/user-info/city/{city}")
    public ResponseEntity<List<UserInfo>> getUsersByCity(@PathVariable String city) {
        try {
            List<UserInfo> users = userInfoService.getUsersByCity(city);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get users by country
     * GET /api/user-info/country/{country}
     */
    @GetMapping("/user-info/country/{country}")
    public ResponseEntity<List<UserInfo>> getUsersByCountry(@PathVariable String country) {
        try {
            List<UserInfo> users = userInfoService.getUsersByCountry(country);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get user statistics
     * GET /api/user-info/stats
     */
    @GetMapping("/user-info/stats")
    public ResponseEntity<Object> getUserStats() {
        try {
            long totalUsers = userInfoService.getTotalUserCount();
            return ResponseEntity.ok(java.util.Map.of(
                "totalUsers", totalUsers,
                "message", "User statistics retrieved successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
