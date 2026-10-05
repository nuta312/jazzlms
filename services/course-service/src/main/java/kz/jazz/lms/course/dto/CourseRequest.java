package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CourseRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 20) String code,
        @Size(max = 5000) String description,
        UUID categoryId,
        BigDecimal price,
        Integer capacity,
        String level,
        Boolean active,
        Boolean hiddenFromCatalog,
        Boolean sequential,
        UUID certificateTemplateId,     // null = без сертификата
        Boolean noCertificate           // true: явно отключить сертификат (иначе при создании ставится шаблон по умолчанию)
) {}
