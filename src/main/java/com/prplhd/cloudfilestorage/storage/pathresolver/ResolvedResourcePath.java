package com.prplhd.cloudfilestorage.storage.pathresolver;

public record ResolvedResourcePath(String parentPath, String resourceName, ResourceType resourceType) {
}
