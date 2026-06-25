package com.umahato.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotNull(message = "id is required")
        Long id,
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,
        @NotBlank(message = "name is required")
        String name,
        @NotBlank(message = "preferences is required")
        String preferences
) {
}
