package com.prplhd.cloudfilestorage.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
