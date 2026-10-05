package kz.jazz.lms.notification.dto;

import jakarta.validation.constraints.*;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.notification.domain.Recipient;

public record NotificationRuleRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull EventType eventType,
        @NotNull Recipient recipient,
        Boolean active,
        @Min(0) @Max(43200) Integer delayMinutes,
        @NotBlank String subjectTemplate,
        @NotBlank String bodyTemplate
) {}
