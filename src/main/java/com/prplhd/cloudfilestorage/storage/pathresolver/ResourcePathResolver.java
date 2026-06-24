package com.prplhd.cloudfilestorage.storage.pathresolver;

public final class ResourcePathResolver {

    private ResourcePathResolver() {}

    public static ResolvedResourcePath resolve(String path) {
        String parentPath = resolveParentPath(path);
        String resourceName = resolveResourceName(path, parentPath);
        ResourceType resourceType = resolveResourceType(path);

        return new ResolvedResourcePath(parentPath, resourceName, resourceType);
    }

    private static String resolveParentPath(String path) {
        String normalizedPath;

        if (path.endsWith("/")) {
            normalizedPath = path.substring(0, path.length() - 1);
        } else {
            normalizedPath = path;
        }

        int lastSlashIndex = normalizedPath.lastIndexOf("/");

        return normalizedPath.substring(0, lastSlashIndex + 1);
    }

    private static String resolveResourceName(String path, String parentPath) {
        int parentPathLength = parentPath.length();

        String resourceName = path.substring(parentPathLength);

        if (resourceName.endsWith("/")) {
            return resourceName.substring(0, resourceName.length() - 1);
        }

        return resourceName;
    }

    private static ResourceType resolveResourceType(String path) {
        if (path.endsWith("/")) {
            return ResourceType.DIRECTORY;
        }

        return ResourceType.FILE;
    }
}
