package kz.jazz.lms.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Настройки портала, вкладки Basic settings и Users экрана Account & Settings.
 * Таблица из одной строки (id = 1): читаем всегда её, обновляем её же.
 */
@Entity
@Table(name = "account_settings")
public class AccountSettings {
    public enum SignupMode { MANUAL, DIRECT }                       // Manually (from Admin) | Direct
    public enum VisibleUserFormat { FIRST_LAST, LAST_FIRST, USERNAME }

    public static final short SINGLETON_ID = 1;

    @Id
    private short id = SINGLETON_ID;

    @Column(name = "site_name", nullable = false) private String siteName = "JazzLMS";
    @Column(name = "site_description") private String siteDescription;
    @Column(name = "default_language", nullable = false) private String defaultLanguage = "en";
    @Column(name = "default_time_zone", nullable = false) private String defaultTimeZone = "UTC";

    @Enumerated(EnumType.STRING) @Column(name = "signup_mode", nullable = false)
    private SignupMode signupMode = SignupMode.DIRECT;
    @Enumerated(EnumType.STRING) @Column(name = "default_user_type", nullable = false)
    private UserType defaultUserType = UserType.LEARNER;
    @Column(name = "default_group_id") private UUID defaultGroupId;

    @Column(name = "password_max_age_days") private Integer passwordMaxAgeDays;
    @Column(name = "password_change_first_login", nullable = false) private boolean passwordChangeOnFirstLogin;
    @Column(name = "lock_after_attempts") private Integer lockAfterAttempts;
    @Column(name = "lock_minutes", nullable = false) private int lockMinutes = 30;

    @Column(name = "terms_of_service") private String termsOfService;
    @Enumerated(EnumType.STRING) @Column(name = "visible_user_format", nullable = false)
    private VisibleUserFormat visibleUserFormat = VisibleUserFormat.FIRST_LAST;

    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    public short getId() { return id; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public String getSiteDescription() { return siteDescription; }
    public void setSiteDescription(String siteDescription) { this.siteDescription = siteDescription; }
    public String getDefaultLanguage() { return defaultLanguage; }
    public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }
    public String getDefaultTimeZone() { return defaultTimeZone; }
    public void setDefaultTimeZone(String defaultTimeZone) { this.defaultTimeZone = defaultTimeZone; }
    public SignupMode getSignupMode() { return signupMode; }
    public void setSignupMode(SignupMode signupMode) { this.signupMode = signupMode; }
    public UserType getDefaultUserType() { return defaultUserType; }
    public void setDefaultUserType(UserType defaultUserType) { this.defaultUserType = defaultUserType; }
    public UUID getDefaultGroupId() { return defaultGroupId; }
    public void setDefaultGroupId(UUID defaultGroupId) { this.defaultGroupId = defaultGroupId; }
    public Integer getPasswordMaxAgeDays() { return passwordMaxAgeDays; }
    public void setPasswordMaxAgeDays(Integer passwordMaxAgeDays) { this.passwordMaxAgeDays = passwordMaxAgeDays; }
    public boolean isPasswordChangeOnFirstLogin() { return passwordChangeOnFirstLogin; }
    public void setPasswordChangeOnFirstLogin(boolean v) { this.passwordChangeOnFirstLogin = v; }
    public Integer getLockAfterAttempts() { return lockAfterAttempts; }
    public void setLockAfterAttempts(Integer lockAfterAttempts) { this.lockAfterAttempts = lockAfterAttempts; }
    public int getLockMinutes() { return lockMinutes; }
    public void setLockMinutes(int lockMinutes) { this.lockMinutes = lockMinutes; }
    public String getTermsOfService() { return termsOfService; }
    public void setTermsOfService(String termsOfService) { this.termsOfService = termsOfService; }
    public VisibleUserFormat getVisibleUserFormat() { return visibleUserFormat; }
    public void setVisibleUserFormat(VisibleUserFormat visibleUserFormat) { this.visibleUserFormat = visibleUserFormat; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
