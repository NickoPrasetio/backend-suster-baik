package com.tukangku.auth.service;

import com.tukangku.auth.dto.AuthResponse;
import com.tukangku.auth.dto.LoginRequest;
import com.tukangku.auth.dto.RegisterRequest;
import com.tukangku.auth.dto.UpdateProfileRequest;
import com.tukangku.auth.entity.UserEntity;
import com.tukangku.auth.repository.UserRepository;
import com.tukangku.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService authService;

    private UserEntity mockUser;

    @BeforeEach
    void setUp() {
        mockUser = UserEntity.builder()
                .id("user-1")
                .name("Budi Santoso")
                .email("budi@example.com")
                .password("encoded-password")
                .phone("081234567890")
                .role("ROLE_USER")
                .userType("CUSTOMER")
                .build();
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("returns AuthResponse with token on success")
        void success_returnsAuthResponseWithToken() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Budi Santoso");
            req.setEmail("budi@example.com");
            req.setPassword("password123");
            req.setUserType("CUSTOMER");

            when(userRepository.existsByEmail("budi@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(UserEntity.class))).thenReturn(mockUser);
            when(jwtUtil.generateToken(any(), any(), any())).thenReturn("jwt-token");

            AuthResponse response = authService.register(req);

            assertThat(response.getEmail()).isEqualTo("budi@example.com");
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getName()).isEqualTo("Budi Santoso");
            verify(userRepository).save(any(UserEntity.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when email already exists")
        void emailAlreadyExists_throwsIllegalArgument() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Budi");
            req.setEmail("budi@example.com");
            req.setPassword("password123");

            when(userRepository.existsByEmail("budi@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sudah terdaftar");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("defaults to CUSTOMER when userType is null")
        void nullUserType_defaultsToCustomer() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Budi");
            req.setEmail("budi@example.com");
            req.setPassword("password123");
            // userType intentionally null

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("enc");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity u = inv.getArgument(0);
                assertThat(u.getUserType()).isEqualTo("CUSTOMER");
                return mockUser;
            });
            when(jwtUtil.generateToken(any(), any(), any())).thenReturn("tok");

            authService.register(req);
        }

        @Test
        @DisplayName("stores userType in UPPER_CASE")
        void userTypeLowerCase_isStoredUpperCase() {
            RegisterRequest req = new RegisterRequest();
            req.setName("Rizky");
            req.setEmail("rizky@example.com");
            req.setPassword("password123");
            req.setUserType("tukang");

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("enc");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity u = inv.getArgument(0);
                assertThat(u.getUserType()).isEqualTo("TUKANG");
                return mockUser;
            });
            when(jwtUtil.generateToken(any(), any(), any())).thenReturn("tok");

            authService.register(req);
        }
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns AuthResponse with JWT on valid credentials")
        void validCredentials_returnsTokenAndProfile() {
            LoginRequest req = new LoginRequest();
            req.setEmail("budi@example.com");
            req.setPassword("password123");

            when(userRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
            when(jwtUtil.generateToken("user-1", "budi@example.com", "ROLE_USER")).thenReturn("jwt-token");

            AuthResponse response = authService.login(req);

            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getName()).isEqualTo("Budi Santoso");
            assertThat(response.getEmail()).isEqualTo("budi@example.com");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when email not found")
        void emailNotFound_throwsIllegalArgument() {
            LoginRequest req = new LoginRequest();
            req.setEmail("notfound@example.com");
            req.setPassword("pass");

            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password salah");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when password does not match")
        void wrongPassword_throwsIllegalArgument() {
            LoginRequest req = new LoginRequest();
            req.setEmail("budi@example.com");
            req.setPassword("wrong-password");

            when(userRepository.findByEmail("budi@example.com")).thenReturn(Optional.of(mockUser));
            when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password salah");
        }
    }

    // ─── getProfile ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("returns user profile without token")
        void success_returnsProfileWithNullToken() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));

            AuthResponse response = authService.getProfile("user-1");

            assertThat(response.getId()).isEqualTo("user-1");
            assertThat(response.getToken()).isNull();
            assertThat(response.getEmail()).isEqualTo("budi@example.com");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown userId")
        void unknownUserId_throwsIllegalArgument() {
            when(userRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.getProfile("bad-id"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }

    // ─── updateProfile ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("updates name and phone when both provided")
        void updatesNameAndPhone() {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setName("Nama Baru");
            req.setPhone("089999999999");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.updateProfile("user-1", req);

            assertThat(response.getName()).isEqualTo("Nama Baru");
            assertThat(response.getPhone()).isEqualTo("089999999999");
        }

        @Test
        @DisplayName("skips name update when name is blank")
        void blankName_keepsOriginalName() {
            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setName("   ");
            req.setPhone("089999999999");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.updateProfile("user-1", req);

            assertThat(response.getName()).isEqualTo("Budi Santoso");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for unknown userId")
        void unknownUserId_throwsIllegalArgument() {
            when(userRepository.findById("no-user")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.updateProfile("no-user", new UpdateProfileRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tidak ditemukan");
        }
    }

    // ─── updateAvatar ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateAvatar()")
    class UpdateAvatar {

        @Test
        @DisplayName("saves new avatar URL and returns updated profile")
        void success_updatesAvatarUrl() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.updateAvatar("user-1", "https://cdn.example.com/avatar.jpg");

            assertThat(response.getAvatar()).isEqualTo("https://cdn.example.com/avatar.jpg");
        }
    }

    // ─── getAllUsers ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("returns mapped list of all users")
        void returnsAllUsers() {
            UserEntity second = UserEntity.builder()
                    .id("user-2").name("Sari").email("sari@example.com")
                    .password("enc").role("ROLE_USER").userType("TUKANG").build();

            when(userRepository.findAll()).thenReturn(List.of(mockUser, second));

            List<AuthResponse> users = authService.getAllUsers();

            assertThat(users).hasSize(2);
            assertThat(users).extracting(AuthResponse::getEmail)
                    .containsExactly("budi@example.com", "sari@example.com");
        }

        @Test
        @DisplayName("returns empty list when no users exist")
        void noUsers_returnsEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(authService.getAllUsers()).isEmpty();
        }
    }
}
