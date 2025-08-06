package com.yourname.filededup.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yourname.filededup.model.UserInfo;

/**
 * Response DTO for user information operations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoResponse {
    
    private boolean success;
    private String message;
    private String userId;
    private UserInfo userInfo;

    // Constructors
    public UserInfoResponse() {}

    public UserInfoResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Static factory methods
    public static UserInfoResponse success(String message, UserInfo userInfo) {
        UserInfoResponse response = new UserInfoResponse(true, message);
        response.setUserId(userInfo.getId());
        response.setUserInfo(userInfo);
        return response;
    }

    public static UserInfoResponse error(String message) {
        return new UserInfoResponse(false, message);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
