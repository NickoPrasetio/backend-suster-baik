package com.tukangku.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for POST /api/auth/google/complete.
 * Called after the user selects their account type on the completion form.
 */
@Data
public class GoogleCompleteRequest {

    /** Google access token — re-validated against userinfo API */
    @NotBlank
    private String accessToken;

    /** CUSTOMER or TUKANG */
    @NotBlank
    @Pattern(regexp = "CUSTOMER|TUKANG", message = "userType harus CUSTOMER atau TUKANG")
    private String userType;

    /** Nomor HP — opsional */
    private String phone;
}
