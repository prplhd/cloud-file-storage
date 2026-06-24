package com.prplhd.cloudfilestorage.dto.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.prplhd.cloudfilestorage.storage.pathresolver.ResourceType;

public record ResourceResponseDto(
        String path,
        String name,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Long size,
        ResourceType type) {
}
