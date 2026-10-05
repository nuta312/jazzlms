package kz.jazz.lms.notification.service;

import kz.jazz.lms.events.DomainEvent;
import kz.jazz.lms.notification.domain.NotificationMessage;
import kz.jazz.lms.notification.domain.NotificationRule;
import kz.jazz.lms.notification.domain.Recipient;
import kz.jazz.lms.notification.dto.NotificationRuleRequest;
import kz.jazz.lms.notification.repository.NotificationMessageRepository;
import kz.jazz.lms.notification.repository.NotificationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRuleRepository rules;
    private final NotificationMessageRepository messages;
    private final String accountOwnerEmail;
    private final String portalName;

    public NotificationService(NotificationRuleRepository rules, NotificationMessageRepository messages,
                               @Value("${lms.account-owner-email}") String accountOwnerEmail,
                               @Value("${lms.portal-name}") String portalName) {
        this.rules = rules;
        this.messages = messages;
        this.accountOwnerEmail = accountOwnerEmail;
        this.portalName = portalName;
    }

    // ---------- обработка события из Kafka ----------

    /**
     * Для каждого активного правила с таким eventType создаём сообщение.
     * delayMinutes == 0 -> отправляем сразу, иначе кладём в PENDING.
     */
    public void handle(DomainEvent event) {
        List<NotificationRule> matched = rules.findByEventTypeAndActiveTrue(event.type());
        log.info("Event {} matched {} rule(s)", event.type(), matched.size());

        Map<String, String> vars = new HashMap<>(event.payload() == null ? Map.of() : event.payload());
        vars.put("portalName", portalName);
        vars.put("eventType", event.type().name());

        for (NotificationRule rule : matched) {
            String to = resolveRecipient(rule.getRecipient(), vars);
            NotificationMessage msg = new NotificationMessage();
            msg.setRuleId(rule.getId());
            msg.setRuleName(rule.getName());
            msg.setEventType(event.type());
            msg.setRecipientEmail(to);
            msg.setSubject(TemplateRenderer.render(rule.getSubjectTemplate(), vars));
            msg.setBody(TemplateRenderer.render(rule.getBodyTemplate(), vars));

            if (rule.getDelayMinutes() > 0) {
                msg.setStatus(NotificationMessage.Status.PENDING);
                msg.setScheduledAt(Instant.now().plusSeconds(rule.getDelayMinutes() * 60L));
                messages.save(msg);
            } else {
                send(msg);
            }
        }
    }

    private String resolveRecipient(Recipient recipient, Map<String, String> vars) {
        return switch (recipient) {
            case RELATED_USER -> vars.getOrDefault("email", "unknown@local");
            case ACCOUNT_OWNER -> accountOwnerEmail;
            case COURSE_INSTRUCTORS -> "instructors-of-" + vars.getOrDefault("courseId", "?") + "@local";
        };
    }

    /**
     * "Отправка" письма. В учебном проекте — просто лог + запись в историю.
     * В реальном проекте здесь был бы JavaMailSender / SendGrid / SES.
     */
    private void send(NotificationMessage msg) {
        log.info("""
                
                ===== EMAIL =====
                To:      {}
                Subject: {}
                {}
                =================""", msg.getRecipientEmail(), msg.getSubject(), msg.getBody());
        msg.setStatus(NotificationMessage.Status.SENT);
        msg.setSentAt(Instant.now());
        messages.save(msg);
    }

    /** Каждые 30 секунд проверяем, не пора ли отправить отложенные. */
    @Scheduled(fixedDelay = 30_000)
    public void flushPending() {
        List<NotificationMessage> due = messages.findByStatusAndScheduledAtBefore(
                NotificationMessage.Status.PENDING, Instant.now());
        if (!due.isEmpty()) log.info("Sending {} scheduled notification(s)", due.size());
        due.forEach(this::send);
    }

    // ---------- CRUD правил ----------

    public List<NotificationRule> allRules() { return rules.findAll(Sort.by("name")); }

    public NotificationRule getRule(String id) {
        return rules.findById(id).orElseThrow(() -> new NotFoundException("Notification not found: " + id));
    }

    public NotificationRule createRule(NotificationRuleRequest req) {
        return rules.save(apply(new NotificationRule(), req));
    }

    public NotificationRule updateRule(String id, NotificationRuleRequest req) {
        return rules.save(apply(getRule(id), req));
    }

    public void deleteRule(String id) { rules.delete(getRule(id)); }

    private NotificationRule apply(NotificationRule r, NotificationRuleRequest req) {
        r.setName(req.name());
        r.setEventType(req.eventType());
        r.setRecipient(req.recipient());
        r.setActive(req.active() == null || req.active());
        r.setDelayMinutes(req.delayMinutes() == null ? 0 : req.delayMinutes());
        r.setSubjectTemplate(req.subjectTemplate());
        r.setBodyTemplate(req.bodyTemplate());
        return r;
    }

    // ---------- история ----------

    public List<NotificationMessage> history() {
        return messages.findByStatus(NotificationMessage.Status.SENT, Sort.by(Sort.Direction.DESC, "sentAt"));
    }

    public List<NotificationMessage> pending() {
        return messages.findByStatus(NotificationMessage.Status.PENDING, Sort.by("scheduledAt"));
    }

    public void clearHistory() { messages.deleteByStatus(NotificationMessage.Status.SENT); }

    public void deleteMessage(String id) { messages.deleteById(id); }
}
