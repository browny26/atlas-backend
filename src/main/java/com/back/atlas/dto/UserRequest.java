package com.back.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String bio;
    private String facebookLink;
    private String instagramLink;
    private String xLink;
    private String linkedinLink;
    private String avatar;
}
