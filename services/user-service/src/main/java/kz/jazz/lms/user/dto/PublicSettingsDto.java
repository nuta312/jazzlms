package kz.jazz.lms.user.dto;

import kz.jazz.lms.user.domain.AccountSettings;

/**
 * Часть настроек, которую видно БЕЗ токена (страницы Login / Sign up):
 * название портала, разрешена ли саморегистрация, текст пользовательского соглашения.
 */
public record PublicSettingsDto(String siteName, boolean signupAllowed, String termsOfService,
                                AccountSettings.VisibleUserFormat visibleUserFormat) {
    public static PublicSettingsDto from(AccountSettings s) {
        return new PublicSettingsDto(s.getSiteName(), s.getSignupMode() == AccountSettings.SignupMode.DIRECT,
                s.getTermsOfService(), s.getVisibleUserFormat());
    }
}
