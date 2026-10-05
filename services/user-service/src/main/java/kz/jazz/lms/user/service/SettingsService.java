package kz.jazz.lms.user.service;

import kz.jazz.lms.user.domain.AccountSettings;
import kz.jazz.lms.user.dto.SettingsDto;
import kz.jazz.lms.user.repository.AccountSettingsRepository;
import kz.jazz.lms.user.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Account & Settings (часть user-service): регистрация, роль по умолчанию, политика паролей, ToS.
 * Строка в БД одна (id = 1), поэтому никакого списка/поиска — только get() и update().
 */
@Service
@Transactional
public class SettingsService {
    private final AccountSettingsRepository repo;
    private final GroupRepository groups;

    public SettingsService(AccountSettingsRepository repo, GroupRepository groups) {
        this.repo = repo;
        this.groups = groups;
    }

    @Transactional(readOnly = true)
    public AccountSettings get() {
        return repo.findById(AccountSettings.SINGLETON_ID).orElseGet(AccountSettings::new);
    }

    public AccountSettings update(SettingsDto dto) {
        AccountSettings s = get();
        if (dto.defaultGroupId() != null && !groups.existsById(dto.defaultGroupId()))
            throw new NotFoundException("Group not found: " + dto.defaultGroupId());
        s.setSiteName(dto.siteName());
        s.setSiteDescription(dto.siteDescription());
        s.setDefaultLanguage(dto.defaultLanguage());
        s.setDefaultTimeZone(dto.defaultTimeZone());
        if (dto.signupMode() != null) s.setSignupMode(dto.signupMode());
        if (dto.defaultUserType() != null) s.setDefaultUserType(dto.defaultUserType());
        s.setDefaultGroupId(dto.defaultGroupId());
        s.setPasswordMaxAgeDays(dto.passwordMaxAgeDays());
        s.setPasswordChangeOnFirstLogin(dto.passwordChangeOnFirstLogin());
        s.setLockAfterAttempts(dto.lockAfterAttempts());
        s.setLockMinutes(dto.lockMinutes());
        s.setTermsOfService(dto.termsOfService() == null || dto.termsOfService().isBlank() ? null : dto.termsOfService());
        if (dto.visibleUserFormat() != null) s.setVisibleUserFormat(dto.visibleUserFormat());
        s.setUpdatedAt(Instant.now());
        return repo.save(s);
    }
}
