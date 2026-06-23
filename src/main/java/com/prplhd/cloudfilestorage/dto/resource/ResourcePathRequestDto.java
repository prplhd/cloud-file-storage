package com.prplhd.cloudfilestorage.dto.resource;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ResourcePathRequestDto(
        @NotNull(message = "Path is required")
        @Pattern(
                regexp = "^(?!/)(?!.*//).*$",
                message = "Path must not start with '/' or contain consecutive '/'"
        )
        String path
) {
}
