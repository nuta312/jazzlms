package kz.jazz.lms.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CertificateTemplateRequest(@NotBlank @Size(max = 100) String name,
                                         @NotBlank @Size(max = 30) String background,
                                         @NotBlank @Size(max = 200) String title,
                                         @NotBlank @Size(max = 5000) String body,
                                         @Size(max = 100) String signature) {}
