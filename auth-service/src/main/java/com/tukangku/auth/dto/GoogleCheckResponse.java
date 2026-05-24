package com.tukangku.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for POST /api/auth/google (check-only, no account creation).
 *
 * If newUser == false  → user exists; token + full profile are populated.
 * If newUser == true   → user not found; only name/email/avatar from Google
 *                        are filled so the frontend can pre-populate the
 *                        completion form.
 */
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class GoogleCheckResponse {

    /** true = user tidak ada di DB → arahkan ke form completion */
    private boolean newUser;

    // ─── Existing user fields (newUser == false) ──────────────────────────────
    private String token;
    private String id;
    private String phone;
    private String role;
    private String userType;
    private Double latitude;
    private Double longitude;

    // ─── Always populated (from Google) ──────────────────────────────────────
    private String name;
    private String email;
    private String avatar;
}
