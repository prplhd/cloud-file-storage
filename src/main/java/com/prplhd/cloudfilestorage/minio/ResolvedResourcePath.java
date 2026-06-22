package com.prplhd.cloudfilestorage.minio;

public record ResolvedResourcePath(String parentPath, String resourceName, ResourceType resourceType, String objectKey) {
}
