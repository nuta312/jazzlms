package kz.jazz.lms.course.web;

import jakarta.validation.Valid;
import kz.jazz.lms.course.domain.Certificate;
import kz.jazz.lms.course.domain.CertificateTemplate;
import kz.jazz.lms.course.dto.CertificateTemplateRequest;
import kz.jazz.lms.course.service.CertificateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *  /api/certificates/templates/**   — шаблоны (чтение: staff, запись: admin — правила в gateway)
 *  /api/certificates/my             — мои сертификаты
 *  /api/certificates/user/{id}      — сертификаты пользователя (staff)
 *  /api/certificates/{id}/view      — HTML сертификата (владелец или staff)
 */
@RestController
@RequestMapping("/api/certificates")
public class CertificateController {
    private final CertificateService service;

    public CertificateController(CertificateService service) { this.service = service; }

    @GetMapping("/templates")
    public List<CertificateTemplate> templates() { return service.templates(); }

    @GetMapping("/templates/backgrounds")
    public List<String> backgrounds() { return List.copyOf(CertificateService.BACKGROUNDS.keySet()); }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public CertificateTemplate create(@Valid @RequestBody CertificateTemplateRequest req) { return service.create(req); }

    @PutMapping("/templates/{id}")
    public CertificateTemplate update(@PathVariable UUID id, @Valid @RequestBody CertificateTemplateRequest req) { return service.update(id, req); }

    @PostMapping("/templates/{id}/reset")
    public CertificateTemplate reset(@PathVariable UUID id) { return service.resetToDefault(id); }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }

    @GetMapping(value = "/templates/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public String preview(@PathVariable UUID id) { return service.preview(id); }

    @GetMapping("/my")
    public List<Map<String, Object>> my(@RequestHeader("X-User-Id") UUID userId) { return service.ofUser(userId); }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> ofUser(@PathVariable UUID userId) { return service.ofUser(userId); }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.toMap(service.get(id, userId, role));
    }

    @GetMapping(value = "/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public String view(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        Certificate c = service.get(id, userId, role);
        return service.render(c);
    }
}
