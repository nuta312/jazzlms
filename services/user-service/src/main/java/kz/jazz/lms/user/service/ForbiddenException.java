package kz.jazz.lms.user.service;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
