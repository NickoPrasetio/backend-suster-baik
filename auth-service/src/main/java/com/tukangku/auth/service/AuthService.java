package com.tukangku.auth.service;

import com.tukangku.auth.dto.AuthResponse;
import com.tukangku.auth.dto.LoginRequest;
import com.tukangku.auth.dto.RegisterRequest;
import com.tukangku.auth.dto.UpdateProfileRequest;
import com.tukangku.auth.entity.UserEntity;
import com.tukangku.auth.repository.UserRepository;
import com.tukangku.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }
        String userType = (request.getUserType() != null && !request.getUserType().isBlank())
                ? request.getUserType().toUpperCase()
                : "CUSTOMER";
        UserEntity user = UserEntity.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role("ROLE_USER")
                .userType(userType)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        user = userRepository.save(user);
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return toResponse(user, token);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email atau password salah"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email atau password salah");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return toResponse(user, token);
    }

    public AuthResponse getProfile(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        return toResponse(user, null);
    }

    public AuthResponse updateProfile(String userId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        return toResponse(userRepository.save(user), null);
    }

    public AuthResponse updateAvatar(String userId, String avatarUrl) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        user.setAvatar(avatarUrl);
        return toResponse(userRepository.save(user), null);
    }

    public List<AuthResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> toResponse(u, null))
                .toList();
    }

    public AuthResponse adminUpdateUser(String targetId, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(targetId)
                .orElseThrow(() -> new IllegalArgumentException("User tidak ditemukan"));
        if (request.getName() != null && !request.getName().isBlank()) user.setName(request.getName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        return toResponse(userRepository.save(user), null);
    }

    private AuthResponse toResponse(UserEntity user, String token) {
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
