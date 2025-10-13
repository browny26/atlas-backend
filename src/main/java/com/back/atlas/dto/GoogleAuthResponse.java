package com.back.atlas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleAuthResponse {
    private String token;
    private String type = "Bearer";
    private UserResponse user;
}
