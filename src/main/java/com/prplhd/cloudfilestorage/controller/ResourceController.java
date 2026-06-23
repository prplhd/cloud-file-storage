package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.resource.DirectoryPathRequestDto;
import com.prplhd.cloudfilestorage.dto.resource.ResourcePathRequestDto;
import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.security.UserPrincipal;
import com.prplhd.cloudfilestorage.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfo(@Valid @ModelAttribute ResourcePathRequestDto requestDto,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        ResourceResponseDto resourceResponseDto = resourceService.getResourceInfo(userId, path);

        return ResponseEntity.ok(resourceResponseDto);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponseDto>> uploadResources(@Valid @ModelAttribute DirectoryPathRequestDto requestDto,
                                                                 @RequestParam("object") List<MultipartFile> files,
                                                                 @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        List<ResourceResponseDto> responseDtos = resourceService.uploadResources(userId, path, files);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDtos);
    }
}
