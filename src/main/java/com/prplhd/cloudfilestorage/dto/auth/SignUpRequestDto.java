package com.prplhd.cloudfilestorage.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequestDto(

        @NotBlank(message = "Username is required")
        @Size(min = 6, max = 255,message = "Username must be between {min} and {max} characters")
        @Pattern(
                regexp = "^[A-Za-z0-9_]+$",
                message = "Username may contain only English letters, digits and underscores"
        )
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 255, message = "Password must be between {min} and {max} characters")
        String password
) {
}
