package com.example.plms.web.dto;

public record AuthResponse(
    String token,
    UserResponse user
) {
}
