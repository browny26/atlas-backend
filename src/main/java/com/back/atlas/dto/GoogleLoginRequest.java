package com.back.atlas.dto;

import lombok.Data;

@Data
public class GoogleLoginRequest {
    private String sub;
    private String email;
    private String name;
    private String picture;
    private String given_name;
    private String family_name;
}
