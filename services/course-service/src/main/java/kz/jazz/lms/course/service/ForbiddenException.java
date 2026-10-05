package kz.jazz.lms.course.service;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String m) { super(m); }
}
