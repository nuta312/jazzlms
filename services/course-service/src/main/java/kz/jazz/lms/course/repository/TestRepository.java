package kz.jazz.lms.course.repository;

import kz.jazz.lms.course.domain.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TestRepository extends JpaRepository<Test, UUID> {}
