package com.tukangku.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body untuk POST /api/auth/facebook/complete.
 * Dipanggil setelah user mengisi form pilih tipe akun.
 */
@Data
public class FacebookCompleteRequest {

    /** Facebook access token — di-re-validasi ke Graph API */
    @NotBlank
    private String accessToken;

    /** CUSTOMER atau TUKANG */
    @NotBlank
    @Pattern(regexp = "CUSTOMER|TUKANG", message = "userType harus CUSTOMER atau TUKANG")
    private String userType;

    /** Nomor HP — opsional */
    private String phone;
}
