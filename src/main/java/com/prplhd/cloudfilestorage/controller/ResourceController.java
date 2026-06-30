package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.resource.*;
import com.prplhd.cloudfilestorage.security.UserPrincipal;
import com.prplhd.cloudfilestorage.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class ResourceController {

    private static final MediaType APPLICATION_ZIP = MediaType.parseMediaType("application/zip");
    private static final String ZIP_FILE_EXTENSION = ".zip";

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

    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(@Valid @ModelAttribute ResourcePathRequestDto requestDto,
                                                                  @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getId();
        String path = requestDto.path();

        ResourceResponseDto downloadedResource = resourceService.getResourceInfo(userId, path);

        StreamingResponseBody responseBody = outputStream -> resourceService.downloadResource(userId, path, outputStream);

        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity.ok();
        String filename;
        MediaType mediaType;

        switch (downloadedResource.type()) {
            case FILE -> {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
                bodyBuilder.contentLength(downloadedResource.size());
                filename = downloadedResource.name();
            }

            case DIRECTORY -> {
                mediaType = APPLICATION_ZIP;
                filename = downloadedResource.name() + ZIP_FILE_EXTENSION;
            }

            default -> throw new IllegalStateException(
                    "Unsupported resource type: " + downloadedResource.type()
            );
        }

        ContentDisposition contentDisposition = ContentDisposition
                .attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return bodyBuilder
                .contentType(mediaType)
                .headers(headers -> headers.setContentDisposition(contentDisposition))
                .body(responseBody);
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
