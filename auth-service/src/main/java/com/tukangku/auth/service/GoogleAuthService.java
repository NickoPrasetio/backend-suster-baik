package com.tukangku.auth.service;

import com.tukangku.auth.dto.AuthResponse;
import com.tukangku.auth.dto.GoogleCheckResponse;
import com.tukangku.auth.dto.GoogleTokenInfo;
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

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final JwtUtil        jwtUtil;
    private final RestTemplate   restTemplate;

    private static final String USERINFO_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. CHECK — hanya cek apakah user sudah ada, TIDAK buat akun baru
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validasi Google access token dan cek keberadaan user di database.
     *
     * • Jika user sudah ada  → return { newUser: false, token, ...profile }
     * • Jika user belum ada  → return { newUser: true, name, email, avatar }
     *   (frontend harus redirect ke halaman completion)
     */
    @Transactional
    public GoogleCheckResponse checkUser(String accessToken) {
        GoogleTokenInfo userInfo = fetchGoogleUserInfo(accessToken);
        validateUserInfo(userInfo);

        String googleId = userInfo.getSub();
        String email    = userInfo.getEmail();
        String name     = userInfo.getName() != null ? userInfo.getName() : email;
        String picture  = userInfo.getPicture();

        Optional<UserEntity> existing = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email));

        if (existing.isEmpty()) {
            // Pengguna baru — kembalikan data Google untuk pre-fill form
            return GoogleCheckResponse.builder()
                    .newUser(true)
                    .name(name)
                    .email(email)
                    .avatar(picture)
                    .build();
        }

        // Pengguna lama — link googleId jika belum ada, update avatar
        UserEntity user = existing.get();
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            user.setProvider("GOOGLE");
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
     * Selesaikan registrasi Google untuk user baru.
     * Re-validasi access token ke Google untuk memastikan tidak ada manipulasi,
     * lalu buat akun dengan userType yang dipilih pengguna.
     */
    @Transactional
    public AuthResponse completeSignup(String accessToken, String userType, String phone) {
        GoogleTokenInfo userInfo = fetchGoogleUserInfo(accessToken);
        validateUserInfo(userInfo);

        String googleId = userInfo.getSub();
        String email    = userInfo.getEmail();
        String name     = userInfo.getName() != null ? userInfo.getName() : email;
        String picture  = userInfo.getPicture();

        // Race-condition safety: jika user sudah ada (misal, daftar ulang), langsung login
        Optional<UserEntity> existing = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email));

        if (existing.isPresent()) {
            UserEntity user = existing.get();
            String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
            return toAuthResponse(user, token);
        }

        UserEntity newUser = UserEntity.builder()
                .name(name)
                .email(email)
                .password(UUID.randomUUID().toString()) // Google user tidak bisa login manual
                .phone(phone != null && !phone.isBlank() ? phone : null)
                .role("ROLE_USER")
                .userType(userType)
                .provider("GOOGLE")
                .googleId(googleId)
                .avatar(picture)
                .build();
        newUser = userRepository.save(newUser);

        String token = jwtUtil.generateToken(newUser.getId(), newUser.getEmail(), newUser.getRole());
        return toAuthResponse(newUser, token);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private GoogleTokenInfo fetchGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<GoogleTokenInfo> response = restTemplate.exchange(
                    USERINFO_URL, HttpMethod.GET, entity, GoogleTokenInfo.class);
            GoogleTokenInfo info = response.getBody();
            if (info == null || info.getSub() == null) {
                throw new IllegalArgumentException("Respons Google tidak valid");
            }
            return info;
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new IllegalArgumentException("Token Google tidak valid atau sudah expired");
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gagal verifikasi Google token: {}", e.getMessage());
            throw new IllegalArgumentException("Gagal menghubungi Google, coba lagi");
        }
    }

    private void validateUserInfo(GoogleTokenInfo userInfo) {
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new IllegalArgumentException("Tidak dapat mengambil email dari akun Google");
        }
        if (!Boolean.TRUE.equals(userInfo.getEmailVerified())) {
            throw new IllegalArgumentException("Email Google belum diverifikasi");
        }
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
