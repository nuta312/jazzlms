package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.jazz.lms.course.domain.Category;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryDto(UUID id, @NotBlank @Size(max = 100) String name, UUID parentId, BigDecimal price) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getParentId(), c.getPrice());
    }
}
