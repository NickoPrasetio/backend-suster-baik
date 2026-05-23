package com.tukangku.auth.controller;

import com.tukangku.auth.dto.AuthResponse;
import com.tukangku.auth.dto.LoginRequest;
import com.tukangku.auth.dto.RegisterRequest;
import com.tukangku.auth.dto.UpdateProfileRequest;
import com.tukangku.auth.service.AuthService;
import com.tukangku.auth.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MinioService minioService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<AuthResponse> updateProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody UpdateProfileRequest request) {
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestPart("file") MultipartFile file) {
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "File kosong"));
        if (file.getSize() > 5 * 1024 * 1024) return ResponseEntity.badRequest().body(Map.of("error", "Maksimal 5MB"));
        if (file.getContentType() == null || !file.getContentType().startsWith("image/"))
            return ResponseEntity.badRequest().body(Map.of("error", "File harus gambar"));
        try {
            String ext = getExt(file.getOriginalFilename());
            String url = minioService.uploadAvatar(file, "user-" + userId + "-" + System.currentTimeMillis() + ext);
            return ResponseEntity.ok(authService.updateAvatar(userId, url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Gagal upload foto"));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<AuthResponse>> getAllUsers(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AuthResponse> adminUpdateUser(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable String id,
            @RequestBody UpdateProfileRequest request) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(authService.adminUpdateUser(id, request));
    }

    @PostMapping(value = "/users/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> adminUploadAvatar(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable String id,
            @RequestPart("file") MultipartFile file) {
        if (!"ROLE_ADMIN".equals(userRole)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) return ResponseEntity.badRequest().build();
        try {
            String ext = getExt(file.getOriginalFilename());
            String url = minioService.uploadAvatar(file, "user-" + id + "-" + System.currentTimeMillis() + ext);
            return ResponseEntity.ok(authService.updateAvatar(id, url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Gagal upload foto"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "auth-service"));
    }

    private String getExt(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
