package kz.jazz.lms.user.service;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
