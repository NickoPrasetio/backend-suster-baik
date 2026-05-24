package com.tukangku.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Mapping respons dari Google userinfo endpoint:
 * GET https://www.googleapis.com/oauth2/v3/userinfo
 * Authorization: Bearer {access_token}
 */
@Data
public class GoogleTokenInfo {
    /** Google subject ID — ID unik user di Google */
    private String sub;

    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    private String name;

    private String picture;
}
