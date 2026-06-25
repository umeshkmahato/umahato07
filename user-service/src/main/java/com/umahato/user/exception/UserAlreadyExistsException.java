package com.umahato.user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(Long userId) {
        super("User already exists for id=" + userId);
    }
}
