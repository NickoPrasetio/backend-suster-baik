package com.tukangku.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tukangku.auth.dto.AuthResponse;
import com.tukangku.auth.dto.LoginRequest;
import com.tukangku.auth.dto.RegisterRequest;
import com.tukangku.auth.service.AuthService;
import com.tukangku.auth.service.FacebookAuthService;
import com.tukangku.auth.service.GoogleAuthService;
import com.tukangku.auth.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    @MockBean GoogleAuthService googleAuthService;
    @MockBean FacebookAuthService facebookAuthService;
    @MockBean MinioService minioService;

    private AuthResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = AuthResponse.builder()
                .id("user-1")
                .name("Budi Santoso")
                .email("budi@example.com")
                .role("ROLE_USER")
                .userType("CUSTOMER")
                .token("jwt-token-xxx")
                .build();
    }

    // ─── POST /api/auth/register ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("200 OK with AuthResponse on valid request")
        void validRequest_returns200() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("Budi Santoso");
            req.setEmail("budi@example.com");
            req.setPassword("password123");

            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("budi@example.com"))
                    .andExpect(jsonPath("$.token").value("jwt-token-xxx"));
        }

        @Test
        @DisplayName("400 Bad Request when email is missing")
        void missingEmail_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("Budi");
            req.setPassword("password123");
            // email is null/blank

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 Bad Request when password is too short")
        void shortPassword_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setName("Budi");
            req.setEmail("budi@example.com");
            req.setPassword("abc"); // less than 6 chars

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("200 OK with token on valid credentials")
        void validCredentials_returns200WithToken() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("budi@example.com");
            req.setPassword("password123");

            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token-xxx"))
                    .andExpect(jsonPath("$.name").value("Budi Santoso"));
        }

        @Test
        @DisplayName("400 Bad Request when email is invalid format")
        void invalidEmail_returns400() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setEmail("not-an-email");
            req.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── GET /api/auth/me ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/auth/me")
    class GetMe {

        @Test
        @DisplayName("200 OK with profile when X-User-Id header is provided")
        void withUserId_returns200() throws Exception {
            when(authService.getProfile("user-1")).thenReturn(mockAuthResponse);

            mockMvc.perform(get("/api/auth/me")
                            .header("X-User-Id", "user-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("user-1"))
                    .andExpect(jsonPath("$.email").value("budi@example.com"));
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── PUT /api/auth/me ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/auth/me")
    class UpdateMe {

        @Test
        @DisplayName("200 OK with updated profile when authenticated")
        void withUserId_returns200() throws Exception {
            when(authService.updateProfile(eq("user-1"), any())).thenReturn(mockAuthResponse);

            mockMvc.perform(put("/api/auth/me")
                            .header("X-User-Id", "user-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Budi Baru\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("401 Unauthorized when X-User-Id header is missing")
        void missingUserId_returns401() throws Exception {
            mockMvc.perform(put("/api/auth/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Budi Baru\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─── GET /api/auth/users (admin) ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/auth/users")
    class GetAllUsers {

        @Test
        @DisplayName("200 OK with user list when role is ROLE_ADMIN")
        void adminRole_returns200() throws Exception {
            when(authService.getAllUsers()).thenReturn(List.of(mockAuthResponse));

            mockMvc.perform(get("/api/auth/users")
                            .header("X-User-Role", "ROLE_ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("budi@example.com"));
        }

        @Test
        @DisplayName("403 Forbidden when role is not ROLE_ADMIN")
        void nonAdminRole_returns403() throws Exception {
            mockMvc.perform(get("/api/auth/users")
                            .header("X-User-Role", "ROLE_USER"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("403 Forbidden when X-User-Role header is missing")
        void missingRole_returns403() throws Exception {
            mockMvc.perform(get("/api/auth/users"))
                    .andExpect(status().isForbidden());
        }
    }

    // ─── GET /api/auth/health ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/auth/health returns 200 OK with service name")
    void healthEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("auth-service"));
    }
}
