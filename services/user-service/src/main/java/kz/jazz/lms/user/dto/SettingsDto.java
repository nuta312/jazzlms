package kz.jazz.lms.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kz.jazz.lms.user.domain.AccountSettings;
import kz.jazz.lms.user.domain.UserType;

import java.util.UUID;

/** Один и тот же record идёт и в ответ GET /api/settings, и в тело PUT /api/settings. */
public record SettingsDto(
        @NotBlank @Size(max = 100) String siteName,
        @Size(max = 500) String siteDescription,
        @NotBlank String defaultLanguage,
        @NotBlank String defaultTimeZone,
        AccountSettings.SignupMode signupMode,
        UserType defaultUserType,
        UUID defaultGroupId,
        @Min(1) @Max(3650) Integer passwordMaxAgeDays,
        boolean passwordChangeOnFirstLogin,
        @Min(1) @Max(100) Integer lockAfterAttempts,
        @Min(1) @Max(100000) int lockMinutes,
        @Size(max = 20000) String termsOfService,
        AccountSettings.VisibleUserFormat visibleUserFormat
) {
    public static SettingsDto from(AccountSettings s) {
        return new SettingsDto(s.getSiteName(), s.getSiteDescription(), s.getDefaultLanguage(), s.getDefaultTimeZone(),
                s.getSignupMode(), s.getDefaultUserType(), s.getDefaultGroupId(), s.getPasswordMaxAgeDays(),
                s.isPasswordChangeOnFirstLogin(), s.getLockAfterAttempts(), s.getLockMinutes(), s.getTermsOfService(),
                s.getVisibleUserFormat());
    }
}
