package kz.jazz.lms.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Ветка (Branch) — под-портал: свой язык, часовой пояс, тип пользователя по умолчанию, объявление. */
@Entity
@Table(name = "branches")
public class Branch {
    public enum SignupMode { MANUAL, DIRECT }

    @Id private UUID id;
    @Column(nullable = false, unique = true) private String name;
    private String title;
    private String description;
    @Column(nullable = false) private String language = "en";
    @Column(name = "time_zone", nullable = false) private String timeZone = "UTC";
    @Enumerated(EnumType.STRING) @Column(name = "default_user_type", nullable = false) private UserType defaultUserType = UserType.LEARNER;
    @Enumerated(EnumType.STRING) @Column(name = "signup_mode", nullable = false) private SignupMode signupMode = SignupMode.MANUAL;
    private String announcement;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    public UserType getDefaultUserType() { return defaultUserType; }
    public void setDefaultUserType(UserType defaultUserType) { this.defaultUserType = defaultUserType; }
    public SignupMode getSignupMode() { return signupMode; }
    public void setSignupMode(SignupMode signupMode) { this.signupMode = signupMode; }
    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
}
