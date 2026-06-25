package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.resource.DirectoryPathRequestDto;
import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.security.UserPrincipal;
import com.prplhd.cloudfilestorage.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/directory")
@RequiredArgsConstructor
public class DirectoryController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<List<ResourceResponseDto>> getDirectoryContents(@Valid @ModelAttribute DirectoryPathRequestDto requestDto,
                                                                          @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        List<ResourceResponseDto> responseDtos = resourceService.getDirectoryContents(userId, path);

        return ResponseEntity.ok(responseDtos);
    }

    @PostMapping
    public ResponseEntity<ResourceResponseDto> createDirectory(@Valid @ModelAttribute DirectoryPathRequestDto requestDto,
                                                               @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        ResourceResponseDto responseDto = resourceService.createDirectory(userId, path);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responseDto);
    }
}
