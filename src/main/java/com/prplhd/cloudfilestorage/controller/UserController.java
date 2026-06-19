package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.auth.UserResponseDto;
import com.prplhd.cloudfilestorage.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponseDto userResponseDto = new UserResponseDto(userPrincipal.getUsername());

        return ResponseEntity.ok(userResponseDto);
    }
}
