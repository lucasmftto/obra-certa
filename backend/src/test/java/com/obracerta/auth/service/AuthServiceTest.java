package com.obracerta.auth.service;

import com.obracerta.auth.domain.User;
import com.obracerta.auth.dto.AuthResponse;
import com.obracerta.auth.dto.LoginRequest;
import com.obracerta.auth.dto.RegisterRequest;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User savedUser;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .password("$2a$10$hashedpassword")
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ------------------------------------------------------------------ //
    //  register                                                            //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("register — should return AuthResponse on success")
    void register_shouldReturnAuthResponse_onSuccess() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "secret123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token-abc");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-token-abc");
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.email()).isEqualTo("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register — should throw BusinessException 409 when email already exists")
    void register_shouldThrowConflict_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "secret123");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    // ------------------------------------------------------------------ //
    //  login                                                               //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("login — should return AuthResponse on success")
    void login_shouldReturnAuthResponse_onSuccess() {
        LoginRequest request = new LoginRequest("john@example.com", "secret123");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("secret123", savedUser.getPassword())).thenReturn(true);
        when(jwtService.generateToken(savedUser)).thenReturn("jwt-token-abc");

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-token-abc");
        assertThat(response.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("login — should throw BusinessException 401 when email not found")
    void login_shouldThrowUnauthorized_whenEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "secret123");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("login — should throw BusinessException 401 when password is wrong")
    void login_shouldThrowUnauthorized_whenPasswordIsWrong() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(savedUser));
        when(passwordEncoder.matches("wrongpassword", savedUser.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
