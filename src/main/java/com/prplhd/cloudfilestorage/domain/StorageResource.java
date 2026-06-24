package com.prplhd.cloudfilestorage.domain;

import lombok.Getter;

@Getter
public class StorageResource {

    private final ResourcePath path;
    private final Long size;

    public StorageResource(ResourcePath path, Long size) {
        validate(path, size);

        this.path = path;
        this.size = size;
    }

    public ResourceType getType() {
        return path.getType();
    }

    private static void validate(ResourcePath path, Long size) {
        if (path == null) {
            throw new IllegalArgumentException("Resource path must not be null");
        }

        if (size == null) {
            throw new IllegalArgumentException("Resource size must not be null");
        }

        if (size < 0) {
            throw new IllegalArgumentException("Resource size must not be negative");
        }
    }
}
