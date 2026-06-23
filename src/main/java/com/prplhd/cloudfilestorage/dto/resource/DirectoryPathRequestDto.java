package com.prplhd.cloudfilestorage.dto.resource;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DirectoryPathRequestDto(
        @NotNull(message = "Path is required")
        @Pattern(
                regexp = "^(?:$|(?!/)(?!.*//).*/)$",
                message = "Path must be empty or end with '/' without leading or consecutive slashes"
        )
        String path
) {
}