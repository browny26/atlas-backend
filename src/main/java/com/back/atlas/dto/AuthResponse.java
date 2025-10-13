package com.back.atlas.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private Long id;
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String avatar;
}
