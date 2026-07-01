package com.prplhd.cloudfilestorage.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ResourcePath {

    private static final String SLASH = "/";
    private static final Pattern FULL_PATH_PATTERN = Pattern.compile("^(?!/)(?!.*//).*$");

    private final String fullPath;

    public ResourcePath(String fullPath) {
        validateFullPath(fullPath);
        this.fullPath = fullPath;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getParentPath() {
        String normalizedPath = pathWithoutTrailingSlash();

        int lastSlashIndex = normalizedPath.lastIndexOf(SLASH);

        return normalizedPath.substring(0, lastSlashIndex + 1);
    }

    public String getName() {
        String normalizedPath = pathWithoutTrailingSlash();

        int lastSlashIndex = normalizedPath.lastIndexOf(SLASH);

        return normalizedPath.substring(lastSlashIndex + 1);
    }

    public ResourceType getType() {
        if (fullPath.endsWith(SLASH) || fullPath.isEmpty()) {
            return ResourceType.DIRECTORY;
        }

        return ResourceType.FILE;
    }

    public List<String> getParentDirectories() {
        String parentPath = getParentPath();

        if (parentPath.isEmpty()) {
            return List.of();
        }

        String[] directories = parentPath.split(SLASH);
        StringBuilder pathAccumulator = new StringBuilder();

        List<String> parentDirectories = new ArrayList<>();

        for (String directory : directories) {
            pathAccumulator.append(directory).append(SLASH);

            parentDirectories.add(pathAccumulator.toString());
        }

        return parentDirectories;
    }

    public boolean isRoot() {
        return fullPath.isEmpty();
    }

    private static void validateFullPath(String fullPath) {
        if (fullPath == null) {
            throw new IllegalArgumentException("Path is required");
        }

        if (!FULL_PATH_PATTERN.matcher(fullPath).matches()) {
            throw new IllegalArgumentException("Path must not start with '%s' or contain consecutive '%s'".formatted(SLASH, SLASH));
        }
    }

    private String pathWithoutTrailingSlash() {
        if (fullPath.endsWith(SLASH)) {
            return fullPath.substring(0, fullPath.length() - 1);
        }

        return fullPath;
    }
}
