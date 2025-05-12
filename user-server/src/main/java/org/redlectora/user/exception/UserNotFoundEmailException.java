package org.redlectora.user.exception;

public class UserNotFoundEmailException extends RuntimeException {
    public UserNotFoundEmailException(String email) {
        super("Usuario con email " + email + " no encontrado");
    }
}