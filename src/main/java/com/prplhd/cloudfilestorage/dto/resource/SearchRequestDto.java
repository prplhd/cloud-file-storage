package com.prplhd.cloudfilestorage.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchRequestDto(
        @NotBlank(message = "Query is required")
        @Size(max = 255, message = "Query must not exceed 255 characters")
        @Pattern(
                regexp = "^(?!/).*(?<!/)$",
                message = "Query must not start or end with '/'"
        )
        String query
) {
}
