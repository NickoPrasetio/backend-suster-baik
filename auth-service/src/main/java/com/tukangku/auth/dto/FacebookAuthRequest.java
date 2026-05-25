package com.tukangku.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FacebookAuthRequest {

    /** Access token dari Facebook Login di frontend */
    @NotBlank
    private String accessToken;
}
