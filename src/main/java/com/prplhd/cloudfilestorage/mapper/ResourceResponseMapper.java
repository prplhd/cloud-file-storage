package com.prplhd.cloudfilestorage.mapper;

import com.prplhd.cloudfilestorage.domain.ResourceType;
import com.prplhd.cloudfilestorage.domain.StorageResource;
import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResourceResponseMapper {

    @Mapping(target = "path", source = "storageResource.path.parentPath")
    @Mapping(target = "name", source = "storageResource.path.name")
    @Mapping(target = "size", expression = "java(resolveSize(storageResource))")
    @Mapping(target = "type", source = "storageResource.type")
    ResourceResponseDto toDto(StorageResource storageResource);

    default Long resolveSize(StorageResource resource) {
        if (resource.getType() == ResourceType.DIRECTORY) {
            return null;
        }

        return resource.getSize();
    }
}
