package com.back.atlas.dto;

import com.back.atlas.entity.User;
import com.back.atlas.enums.AuthProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String bio;
    private String facebookLink;
    private String instagramLink;
    private String xLink;
    private String linkedinLink;
    private String avatar;
    private AuthProvider provider;
    private String createdAt;

    public UserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.phoneNumber = user.getPhoneNumber();
        this.bio = user.getBio();
        this.facebookLink = user.getFacebookLink();
        this.instagramLink = user.getInstagramLink();
        this.xLink = user.getXLink();
        this.linkedinLink = user.getLinkedinLink();
        this.avatar = user.getAvatar();
        this.provider = user.getProvider();
        if (user.getCreatedAt() != null) {
            this.createdAt = user.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
