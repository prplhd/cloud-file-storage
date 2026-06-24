package com.prplhd.cloudfilestorage.mapper;

import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.storage.pathresolver.ResolvedResourcePath;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ResourceResponseMapper {

    @Mapping(target = "path", source = "resourcePath.parentPath")
    @Mapping(target = "name", source = "resourcePath.resourceName")
    @Mapping(target = "size", source = "size")
    @Mapping(target = "type", source = "resourcePath.resourceType")
    ResourceResponseDto toDto(ResolvedResourcePath resourcePath, Long size);
}
