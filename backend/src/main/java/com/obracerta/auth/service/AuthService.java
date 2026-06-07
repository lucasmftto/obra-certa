package com.obracerta.auth.service;

import com.obracerta.auth.domain.User;
import com.obracerta.auth.dto.AuthResponse;
import com.obracerta.auth.dto.LoginRequest;
import com.obracerta.auth.dto.RegisterRequest;
import com.obracerta.auth.repository.UserRepository;
import com.obracerta.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("E-mail já cadastrado.", HttpStatus.CONFLICT);
        }
        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .build();
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), user.getName(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException("Credenciais inválidas.", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("Credenciais inválidas.", HttpStatus.UNAUTHORIZED);
        }
        return new AuthResponse(jwtService.generateToken(user), user.getName(), user.getEmail());
    }
}
