package com.back.atlas.dto;

import com.google.gson.annotations.SerializedName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Only the Google access token: identity data is fetched from Google by the backend. */
@Data
public class GoogleLoginRequest {
    @NotBlank
    @SerializedName(value = "access_token", alternate = "accessToken")
    private String accessToken;
}
