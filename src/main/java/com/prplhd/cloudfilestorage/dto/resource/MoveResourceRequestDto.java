package com.prplhd.cloudfilestorage.dto.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MoveResourceRequestDto(
        @NotBlank(message = "'From' attribute is required")
        @Pattern(
                regexp = "^(?!/)(?!.*//).+$",
                message = "'From' attribute must not start with '/' or contain consecutive '/'"
        )
        String from,

        @NotBlank(message = "'To' attribute is required")
        @Pattern(
                regexp = "^(?!/)(?!.*//).+$",
                message = "'To' attribute must not start with '/' or contain consecutive '/'"
        )
        String to
) {
}
