package kz.jazz.lms.user.service;

public class LockedException extends RuntimeException {
    public LockedException(String message) { super(message); }
}
