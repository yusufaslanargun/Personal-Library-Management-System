package com.example.plms.service;

import com.example.plms.domain.AppUser;
import com.example.plms.repository.AppUserRepository;
import com.example.plms.security.AuthenticatedUser;
import com.example.plms.security.JwtService;
import com.example.plms.web.dto.AuthLoginRequest;
import com.example.plms.web.dto.AuthRegisterRequest;
import com.example.plms.web.dto.AuthResponse;
import com.example.plms.web.dto.UserResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), request.displayName().trim());
        AppUser saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail());
        return new AuthResponse(token, toResponse(saved));
    }

    @Transactional
    public AuthResponse login(AuthLoginRequest request) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        user.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        AppUser saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId(), saved.getEmail());
        return new AuthResponse(token, toResponse(saved));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        AppUser existing = userRepository.findById(user.id())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toResponse(existing);
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt(), user.getLastLoginAt());
    }
}
