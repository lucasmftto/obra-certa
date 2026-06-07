package com.obracerta.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.obracerta.auth.dto.AuthResponse;
import com.obracerta.auth.dto.LoginRequest;
import com.obracerta.auth.dto.RegisterRequest;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.auth.service.AuthService;
import com.obracerta.auth.service.JwtService;
import com.obracerta.shared.config.SecurityConfig;
import com.obracerta.shared.exception.BusinessException;
import com.obracerta.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:4200",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration-ms=604800000"
})
@WithMockUser
@DisplayName("AuthController — WebMvc Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    private AuthResponse authResponseStub() {
        return new AuthResponse("jwt-token-abc", "John Doe", "john@example.com");
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/auth/register                                         //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /auth/register — deve retornar 201 com token")
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "secret123");
        when(authService.register(any())).thenReturn(authResponseStub());

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("jwt-token-abc"))
            .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 409 quando email já existe")
    void register_shouldReturn409_whenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "secret123");
        when(authService.register(any())).thenThrow(new BusinessException("E-mail já cadastrado.", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado."));
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 422 quando campos inválidos")
    void register_shouldReturn422_whenInvalidFields() throws Exception {
        String json = """
            {"name": "", "email": "not-an-email", "password": "123"}
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.campos").isArray());
    }

    // ------------------------------------------------------------------ //
    //  POST /api/v1/auth/login                                            //
    // ------------------------------------------------------------------ //

    @Test
    @DisplayName("POST /auth/login — deve retornar 200 com token")
    void login_shouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "secret123");
        when(authService.login(any())).thenReturn(authResponseStub());

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token-abc"))
            .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("POST /auth/login — deve retornar 401 quando credenciais inválidas")
    void login_shouldReturn401_whenInvalidCredentials() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");
        when(authService.login(any())).thenThrow(new BusinessException("Credenciais inválidas.", HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas."));
    }
}
