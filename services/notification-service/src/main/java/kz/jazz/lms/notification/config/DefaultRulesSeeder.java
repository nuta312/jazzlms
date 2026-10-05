package kz.jazz.lms.notification.config;

import kz.jazz.lms.events.EventType;
import kz.jazz.lms.notification.domain.NotificationRule;
import kz.jazz.lms.notification.domain.Recipient;
import kz.jazz.lms.notification.repository.NotificationRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** Набор правил по умолчанию — как в свежем аккаунте TalentLMS. */
@Configuration
public class DefaultRulesSeeder {

    @Bean
    CommandLineRunner seedRules(NotificationRuleRepository repo) {
        return args -> {
            if (repo.count() > 0) return;
            repo.saveAll(List.of(
                    rule("User signup", EventType.USER_SIGNED_UP, Recipient.RELATED_USER, true, 0,
                            "Your account for {{portalName}} is ready!",
                            "Hi {{firstName}}, welcome to {{portalName}}! Your username is {{username}}."),
                    rule("User signup (email to account owner)", EventType.USER_SIGNED_UP, Recipient.ACCOUNT_OWNER, true, 0,
                            "New signup: {{firstName}} {{lastName}}",
                            "{{firstName}} {{lastName}} ({{email}}) just signed up."),
                    rule("User addition (from an admin)", EventType.USER_CREATED, Recipient.RELATED_USER, true, 0,
                            "Your account for {{portalName}} is ready!",
                            "Hi {{firstName}}, an administrator created an account for you. Username: {{username}}."),
                    rule("Course assignment to learner", EventType.USER_ENROLLED, Recipient.RELATED_USER, true, 0,
                            "You were added to \"{{courseName}}\"",
                            "Hi {{firstName}}, you are now enrolled in {{courseName}}. Good luck!"),
                    rule("Course completion", EventType.COURSE_COMPLETED, Recipient.RELATED_USER, false, 0,
                            "Congratulations! You completed {{courseName}}",
                            "Hi {{firstName}}, you have completed the course {{courseName}}."),
                    rule("Assignment submission", EventType.ASSIGNMENT_SUBMITTED, Recipient.COURSE_INSTRUCTORS, true, 0,
                            "New assignment submitted in {{courseName}}",
                            "{{firstName}} {{lastName}} submitted an assignment."),
                    rule("Reminder 1 hour after signup", EventType.USER_SIGNED_UP, Recipient.RELATED_USER, false, 60,
                            "Have you tried your first course yet?",
                            "Hi {{firstName}}, it's been an hour since you joined {{portalName}}. Check out the catalog!")
            ));
        };
    }

    private static NotificationRule rule(String name, EventType type, Recipient to, boolean active, int delay,
                                         String subject, String body) {
        NotificationRule r = new NotificationRule();
        r.setName(name);
        r.setEventType(type);
        r.setRecipient(to);
        r.setActive(active);
        r.setDelayMinutes(delay);
        r.setSubjectTemplate(subject);
        r.setBodyTemplate(body);
        return r;
    }
}
