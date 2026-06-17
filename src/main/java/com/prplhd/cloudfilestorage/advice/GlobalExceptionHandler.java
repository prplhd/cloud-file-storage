package com.prplhd.cloudfilestorage.advice;

import com.prplhd.cloudfilestorage.dto.ErrorDto;
import com.prplhd.cloudfilestorage.exception.InvalidCredentialsException;
import com.prplhd.cloudfilestorage.exception.InvalidRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ErrorDto errorDto = new ErrorDto(message);

        return new ResponseEntity<>(errorDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorDto> handleInvalidCredentialsException(InvalidCredentialsException e) {
        ErrorDto errorDto = new ErrorDto(e.getMessage());

        return new ResponseEntity<>(errorDto, HttpStatus.UNAUTHORIZED);
    }
}
