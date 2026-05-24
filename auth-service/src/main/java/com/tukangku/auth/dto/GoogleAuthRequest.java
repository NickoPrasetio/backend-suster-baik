package com.tukangku.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    /** Access token dari Google Sign-In di frontend */
    @NotBlank
    private String accessToken;
}
