package kz.jazz.lms.course.dto;

import kz.jazz.lms.course.domain.Course;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CourseDto(
        UUID id, String name, String code, String description,
        UUID categoryId, String categoryName,
        BigDecimal price, Integer capacity, String level,
        boolean active, boolean hiddenFromCatalog,
        UUID createdBy, Instant createdAt, Instant updatedAt,
        long learnersCount,
        boolean sequential,
        UUID certificateTemplateId
) {
    public static CourseDto from(Course c, String categoryName, long learnersCount) {
        return new CourseDto(c.getId(), c.getName(), c.getCode(), c.getDescription(), c.getCategoryId(), categoryName,
                c.getPrice(), c.getCapacity(), c.getLevel(), c.isActive(), c.isHiddenFromCatalog(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt(), learnersCount, c.isSequential(), c.getCertificateTemplateId());
    }
}
