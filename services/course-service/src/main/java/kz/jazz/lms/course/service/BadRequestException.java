package kz.jazz.lms.course.service;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String m) { super(m); }
}
