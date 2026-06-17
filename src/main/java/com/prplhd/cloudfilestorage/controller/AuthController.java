package com.prplhd.cloudfilestorage.controller;

import com.prplhd.cloudfilestorage.dto.auth.SignInRequestDto;
import com.prplhd.cloudfilestorage.dto.auth.SignUpRequestDto;
import com.prplhd.cloudfilestorage.dto.auth.UserResponseDto;
import com.prplhd.cloudfilestorage.security.SessionAuthenticationService;
import com.prplhd.cloudfilestorage.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final SessionAuthenticationService sessionAuthenticationService;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponseDto> signUp(@RequestBody @Valid SignUpRequestDto signUpRequestDto,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response
    ) {
        String username = signUpRequestDto.username();
        String password = signUpRequestDto.password();

        registrationService.register(username, password);

        Authentication authentication = sessionAuthenticationService.authenticateAndCreateSession(
                username,
                password,
                request,
                response
        );

        UserResponseDto userResponseDto = new UserResponseDto(authentication.getName());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userResponseDto);
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserResponseDto> signIn(@RequestBody @Valid SignInRequestDto signInRequestDto,
                                                  HttpServletRequest request,
                                                  HttpServletResponse response
    ) {
        String username = signInRequestDto.username();
        String password = signInRequestDto.password();

        Authentication authentication  = sessionAuthenticationService.authenticateAndCreateSession(
                    username,
                    password,
                    request,
                    response
        );

        UserResponseDto userResponseDto = new UserResponseDto(authentication.getName());

        return ResponseEntity.ok(userResponseDto);
    }
}
