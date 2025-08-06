package com.yourname.filededup.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * MongoDB document model for user information
 */
@Document(collection = "users")
public class UserInfo {
    
    @Id
    private String id;
    
    @Indexed
    private String name;
    
    @Indexed(unique = true)
    private String email;
    
    private String phone;
    private String address;
    private String city;
    private String country;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    // Constructors
    public UserInfo() {
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    public UserInfo(String name, String email, String phone, String address, String city, String country) {
        this();
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.country = country;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedDate = LocalDateTime.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.updatedDate = LocalDateTime.now();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedDate = LocalDateTime.now();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        this.updatedDate = LocalDateTime.now();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
        this.updatedDate = LocalDateTime.now();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
        this.updatedDate = LocalDateTime.now();
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}
