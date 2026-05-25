package com.tukangku.auth.service;

import com.tukangku.auth.dto.AuthResponse;
import com.tukangku.auth.dto.FacebookTokenInfo;
import com.tukangku.auth.dto.GoogleCheckResponse;
import com.tukangku.auth.entity.UserEntity;
import com.tukangku.auth.repository.UserRepository;
import com.tukangku.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookAuthService {

    private final UserRepository userRepository;
    private final JwtUtil        jwtUtil;
    private final RestTemplate   restTemplate;

    private static final String GRAPH_API_URL =
            "https://graph.facebook.com/me";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CHECK — cek keberadaan user, TIDAK buat akun baru
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validasi Facebook access token dan cek apakah user sudah terdaftar.
     *
     * • Jika user sudah ada  → return { newUser: false, token, ...profile }
     * • Jika user belum ada  → return { newUser: true, name, email, avatar }
     */
    @Transactional
    public GoogleCheckResponse checkUser(String accessToken) {
        FacebookTokenInfo userInfo = fetchFacebookUserInfo(accessToken);
        validateUserInfo(userInfo);

        String facebookId = userInfo.getId();
        String email      = userInfo.getEmail();
        String name       = userInfo.getName() != null ? userInfo.getName() : email;
        String picture    = userInfo.getPictureUrl();

        Optional<UserEntity> existing = userRepository.findByFacebookId(facebookId)
                .or(() -> email != null ? userRepository.findByEmail(email) : Optional.empty());

        if (existing.isEmpty()) {
            // Pengguna baru
            return GoogleCheckResponse.builder()
                    .newUser(true)
                    .name(name)
                    .email(email)
                    .avatar(picture)
                    .build();
        }

        // Pengguna lama — link facebookId jika belum ada, update avatar
        UserEntity user = existing.get();
        if (user.getFacebookId() == null) {
            user.setFacebookId(facebookId);
            if ("LOCAL".equals(user.getProvider())) {
                user.setProvider("FACEBOOK");
            }
        }
        if ((user.getAvatar() == null || user.getAvatar().isBlank()) && picture != null) {
            user.setAvatar(picture);
        }
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return GoogleCheckResponse.builder()
                .newUser(false)
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. COMPLETE — buat akun baru setelah user mengisi form completion
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Selesaikan registrasi Facebook untuk user baru.
     * Re-validasi token ke Facebook Graph API lalu buat akun.
     */
    @Transactional
    public AuthResponse completeSignup(String accessToken, String userType, String phone) {
        FacebookTokenInfo userInfo = fetchFacebookUserInfo(accessToken);
        validateUserInfo(userInfo);

        String facebookId = userInfo.getId();
        String email      = userInfo.getEmail();
        String name       = userInfo.getName() != null ? userInfo.getName() : email;
        String picture    = userInfo.getPictureUrl();

        // Race-condition safety
        Optional<UserEntity> existing = userRepository.findByFacebookId(facebookId)
                .or(() -> email != null ? userRepository.findByEmail(email) : Optional.empty());

        if (existing.isPresent()) {
            UserEntity user = existing.get();
            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            return toAuthResponse(user, token);
        }

        // Email wajib ada untuk membuat akun
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Facebook tidak memberikan akses email. Harap hubungkan email ke akun Facebook Anda.");
        }

        UserEntity newUser = UserEntity.builder()
                .name(name)
                .email(email)
                .password(UUID.randomUUID().toString())
                .phone(phone != null && !phone.isBlank() ? phone : null)
                .role("ROLE_USER")
                .userType(userType)
                .provider("FACEBOOK")
                .facebookId(facebookId)
                .avatar(picture)
                .build();
        newUser = userRepository.save(newUser);

        String token = jwtUtil.generateToken(newUser.getId(), newUser.getEmail(), newUser.getRole());
        return toAuthResponse(newUser, token);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private FacebookTokenInfo fetchFacebookUserInfo(String accessToken) {
        String url = UriComponentsBuilder.fromHttpUrl(GRAPH_API_URL)
                .queryParam("fields", "id,name,email,picture.type(large)")
                .queryParam("access_token", accessToken)
                .toUriString();

        try {
            ResponseEntity<FacebookTokenInfo> response = restTemplate.getForEntity(
                    url, FacebookTokenInfo.class);
            FacebookTokenInfo info = response.getBody();
            if (info == null || info.getId() == null) {
                throw new IllegalArgumentException("Respons Facebook tidak valid");
            }
            return info;
        } catch (HttpClientErrorException e) {
            log.warn("Facebook token tidak valid: {} {}", e.getStatusCode(), e.getMessage());
            throw new IllegalArgumentException("Token Facebook tidak valid atau sudah expired");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gagal verifikasi Facebook token: {}", e.getMessage());
            throw new IllegalArgumentException("Gagal menghubungi Facebook, coba lagi");
        }
    }

    private void validateUserInfo(FacebookTokenInfo userInfo) {
        if (userInfo.getId() == null || userInfo.getId().isBlank()) {
            throw new IllegalArgumentException("Tidak dapat mengambil ID dari akun Facebook");
        }
        // Email bisa null — akan divalidasi di completeSignup jika diperlukan
    }

    private AuthResponse toAuthResponse(UserEntity user, String token) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .userType(user.getUserType())
                .latitude(user.getLatitude())
                .longitude(user.getLongitude())
                .build();
    }
}
