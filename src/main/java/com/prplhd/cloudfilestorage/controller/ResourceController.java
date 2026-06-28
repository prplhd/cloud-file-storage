package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.resource.*;
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

    @GetMapping("/search")
    public ResponseEntity<List<ResourceResponseDto>> searchResources(@Valid @ModelAttribute SearchRequestDto requestDto,
                                                                     @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String query = requestDto.query();

        List<ResourceResponseDto> resourceResponseDtos = resourceService.searchResources(userId, query);

        return ResponseEntity.ok(resourceResponseDtos);
    }

    @PostMapping("/move")
    public ResponseEntity<ResourceResponseDto> moveResource(@Valid @ModelAttribute MoveResourceRequestDto requestDto,
                                                                  @AuthenticationPrincipal UserPrincipal userPrincipal)
    {
        Long userId = userPrincipal.getId();
        String sourcePath = requestDto.from();
        String targetPath = requestDto.to();

        ResourceResponseDto resourceResponseDtos = resourceService.moveResource(userId, sourcePath, targetPath);

        return ResponseEntity.ok(resourceResponseDtos);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceResponseDto>> uploadResources(@Valid @ModelAttribute DirectoryPathRequestDto requestDto,
                                                                     @RequestParam("object") List<MultipartFile> files,
                                                                     @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        List<ResourceResponseDto> resourceResponseDtos = resourceService.uploadResources(userId, path, files);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resourceResponseDtos);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteResource(@Valid @ModelAttribute ResourcePathRequestDto requestDto,
                                               @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        resourceService.deleteResource(userId, path);

        return ResponseEntity.noContent().build();
    }
}
