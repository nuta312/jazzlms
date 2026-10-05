package kz.jazz.lms.notification.web;

import jakarta.validation.Valid;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.notification.domain.NotificationMessage;
import kz.jazz.lms.notification.domain.NotificationRule;
import kz.jazz.lms.notification.domain.Recipient;
import kz.jazz.lms.notification.dto.NotificationRuleRequest;
import kz.jazz.lms.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    // --- справочники для формы "Add notification" ---
    @GetMapping("/events")
    public EventType[] events() { return EventType.values(); }

    @GetMapping("/recipients")
    public Recipient[] recipients() { return Recipient.values(); }

    // --- правила ---
    @GetMapping
    public List<NotificationRule> rules() { return service.allRules(); }

    @GetMapping("/{id}")
    public NotificationRule rule(@PathVariable String id) { return service.getRule(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationRule create(@Valid @RequestBody NotificationRuleRequest req) { return service.createRule(req); }

    @PutMapping("/{id}")
    public NotificationRule update(@PathVariable String id, @Valid @RequestBody NotificationRuleRequest req) {
        return service.updateRule(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) { service.deleteRule(id); }

    // --- история и очередь ---
    @GetMapping("/history")
    public List<NotificationMessage> history() { return service.history(); }

    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory() { service.clearHistory(); }

    @GetMapping("/pending")
    public List<NotificationMessage> pending() { return service.pending(); }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable String id) { service.deleteMessage(id); }
}
