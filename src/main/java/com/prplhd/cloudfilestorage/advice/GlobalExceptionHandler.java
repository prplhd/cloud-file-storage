package com.prplhd.cloudfilestorage.advice;

import com.prplhd.cloudfilestorage.dto.ErrorResponseDto;
import com.prplhd.cloudfilestorage.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        ErrorResponseDto errorResponseDto = new ErrorResponseDto(message);

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRequestException(InvalidRequestException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentialsException(InvalidCredentialsException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleUserAlreadyExistsException(UserAlreadyExistsException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceAlreadyExistsException(ResourceAlreadyExistsException e) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(e.getMessage());

        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("Upload failed: {}", e.getMostSpecificCause().getMessage());

        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Upload exceeds the allowed size or file count limit");

        return new ResponseEntity<>(errorResponseDto, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponseDto> handleMinioStorageException(StorageException e) {
        log.error("Storage operation failed", e);

        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Internal server error. Please try again later");

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e) {
        log.error("Unhandled exception", e);

        ErrorResponseDto errorResponseDto = new ErrorResponseDto("Internal server error. Please try again later");

        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
