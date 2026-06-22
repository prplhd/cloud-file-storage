package com.prplhd.cloudfilestorage.minio;

import org.springframework.stereotype.Component;

@Component
public class ResourcePathResolver {

    private static final String USER_ROOT_PREFIX_TEMPLATE = "user-%d-files/";

    public ResolvedResourcePath resolve(Long userId, String path) {
        String parentPath = resolveParentPath(path);
        String resourceName = resolveResourceName(path, parentPath);
        ResourceType resourceType = resolveResourceType(path);
        String objectKey = resolveObjectKey(userId, path);

        return new ResolvedResourcePath(parentPath, resourceName, resourceType, objectKey);
    }

    private String resolveParentPath(String path) {
        String normalizedPath;

        if (path.endsWith("/")) {
            normalizedPath = path.substring(0, path.length() - 1);
        } else {
            normalizedPath = path;
        }

        int lastSlashIndex = normalizedPath.lastIndexOf("/");

        return normalizedPath.substring(0, lastSlashIndex + 1);
    }

    private String resolveResourceName(String path, String parentPath) {
        int parentPathLength = parentPath.length();

        String resourceName = path.substring(parentPathLength);

        if (resourceName.endsWith("/")) {
            return resourceName.substring(0, resourceName.length() - 1);
        }

        return resourceName;
    }

    private ResourceType resolveResourceType(String path) {
        if (path.endsWith("/")) {
            return ResourceType.DIRECTORY;
        }

        return ResourceType.FILE;
    }

    private String resolveObjectKey(Long userId, String path) {
        return USER_ROOT_PREFIX_TEMPLATE.formatted(userId) + path;
    }
}
