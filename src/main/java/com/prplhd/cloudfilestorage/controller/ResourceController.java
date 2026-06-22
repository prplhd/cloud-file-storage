package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.resource.ResourceRequestDto;
import com.prplhd.cloudfilestorage.dto.resource.ResourceResponseDto;
import com.prplhd.cloudfilestorage.security.UserPrincipal;
import com.prplhd.cloudfilestorage.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public ResponseEntity<ResourceResponseDto> getInfo(@Valid @ModelAttribute ResourceRequestDto requestDto,
                                                       @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        ResourceResponseDto resourceResponseDto = resourceService.getResourceInfo(userId, path);

        return ResponseEntity.ok(resourceResponseDto);
    }
}
