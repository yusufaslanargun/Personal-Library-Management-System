package com.example.plms.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRegisterRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String displayName
) {
}
