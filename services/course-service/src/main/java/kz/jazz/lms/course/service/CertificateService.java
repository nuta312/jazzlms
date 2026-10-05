package kz.jazz.lms.course.service;

import kz.jazz.lms.course.client.UserClient;
import kz.jazz.lms.course.domain.Certificate;
import kz.jazz.lms.course.domain.CertificateTemplate;
import kz.jazz.lms.course.domain.Course;
import kz.jazz.lms.course.dto.CertificateTemplateRequest;
import kz.jazz.lms.course.repository.CertificateRepository;
import kz.jazz.lms.course.repository.CertificateTemplateRepository;
import kz.jazz.lms.course.repository.CourseRepository;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.events.Topics;
import kz.jazz.lms.grpc.user.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сертификаты. Выдаются автоматически при завершении курса, если у курса задан шаблон.
 * Рендер — обычный HTML со встроенным CSS: браузер печатает его в PDF (Ctrl+P), внешних библиотек нет.
 */
@Service
@Transactional
public class CertificateService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
    private static final SecureRandom RND = new SecureRandom();

    /** Пресеты фона: ключ -> CSS рамки/фона. Нарисованы стилями, чтобы не хранить картинки. */
    public static final Map<String, String> BACKGROUNDS = new LinkedHashMap<>();
    static {
        BACKGROUNDS.put("classic", "background:#fff;border:14px double #8a9a7b;outline:2px solid #8a9a7b;outline-offset:-24px;");
        BACKGROUNDS.put("plain", "background:#fff;border:3px solid #444;outline:1px solid #444;outline-offset:-10px;");
        BACKGROUNDS.put("floral", "background:#fff;border:12px solid #7fb3a1;border-image:repeating-linear-gradient(45deg,#7fb3a1 0 10px,#cfe8dd 10px 20px) 12;");
        BACKGROUNDS.put("silver", "background:linear-gradient(135deg,#f7f7f7,#dcdcdc);border:8px solid #b9b9b9;outline:1px dashed #d0424f;outline-offset:-16px;");
        BACKGROUNDS.put("gold", "background:#fffdf5;border:10px solid #d9b45c;outline:2px solid #d9b45c;outline-offset:-20px;box-shadow:inset 0 0 0 14px #fff;");
        BACKGROUNDS.put("navy", "background:#fff;border:6px solid #1f3b73;outline:2px solid #1f3b73;outline-offset:-14px;");
        BACKGROUNDS.put("ornament", "background:#fff;border:10px groove #a6b8a0;outline:1px solid #a6b8a0;outline-offset:-18px;");
        BACKGROUNDS.put("modern", "background:linear-gradient(120deg,#e8f0ff 0 30%,#fff 30%);border:0;box-shadow:inset 0 0 0 6px #2f5fd8;");
    }

    private final CertificateTemplateRepository templates;
    private final CertificateRepository certificates;
    private final CourseRepository courses;
    private final UserClient users;
    private final CourseEventPublisher events;

    public CertificateService(CertificateTemplateRepository templates, CertificateRepository certificates,
                              CourseRepository courses, UserClient users, CourseEventPublisher events) {
        this.templates = templates; this.certificates = certificates; this.courses = courses; this.users = users; this.events = events;
    }

    // ---------- templates (Account & Settings → Certificates) ----------

    @Transactional(readOnly = true)
    public List<CertificateTemplate> templates() { return templates.findAllByOrderByCreatedAtAsc(); }

    @Transactional(readOnly = true)
    public CertificateTemplate template(UUID id) {
        return templates.findById(id).orElseThrow(() -> new NotFoundException("Certificate template not found: " + id));
    }

    public CertificateTemplate create(CertificateTemplateRequest req) {
        CertificateTemplate t = new CertificateTemplate();
        apply(t, req);
        return templates.save(t);
    }

    public CertificateTemplate update(UUID id, CertificateTemplateRequest req) {
        CertificateTemplate t = template(id);
        apply(t, req);
        return templates.save(t);
    }

    /** "Reset to default template": вернуть заголовок, текст и фон к значениям по умолчанию. */
    public CertificateTemplate resetToDefault(UUID id) {
        CertificateTemplate t = template(id);
        t.setBackground("classic");
        t.setTitle(CertificateTemplate.DEFAULT_TITLE);
        t.setBody(CertificateTemplate.DEFAULT_BODY);
        t.setSignature("JazzLMS Academy");
        t.setUpdatedAt(Instant.now());
        return templates.save(t);
    }

    public void delete(UUID id) {
        CertificateTemplate t = template(id);
        if (t.isDefault()) throw new ConflictException("The default template cannot be deleted");
        // FK с ON DELETE SET NULL: у курсов с этим шаблоном сертификат просто отключится
        templates.delete(t);
    }

    private void apply(CertificateTemplate t, CertificateTemplateRequest req) {
        if (!BACKGROUNDS.containsKey(req.background())) throw new BadRequestException("Unknown background: " + req.background());
        t.setName(req.name()); t.setBackground(req.background()); t.setTitle(req.title()); t.setBody(req.body());
        t.setSignature(req.signature()); t.setUpdatedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public UUID defaultTemplateId() {
        return templates.findFirstByIsDefaultTrue().map(CertificateTemplate::getId).orElse(null);
    }

    // ---------- issuing ----------

    /** Вызывается из CourseService при 100% прогресса. Повторно не выдаётся (UNIQUE user_id + course_id). */
    public Optional<Certificate> issue(Course course, UUID userId, UserResponse u) {
        if (course.getCertificateTemplateId() == null) return Optional.empty();
        if (certificates.findByUserIdAndCourseId(userId, course.getId()).isPresent()) return Optional.empty();
        Certificate c = new Certificate();
        c.setUserId(userId);
        c.setCourseId(course.getId());
        c.setTemplateId(course.getCertificateTemplateId());
        c.setCode(newCode());
        Certificate saved = certificates.save(c);

        Map<String, String> payload = new HashMap<>();
        payload.put("courseId", course.getId().toString()); payload.put("courseName", course.getName());
        payload.put("userId", userId.toString()); payload.put("certificateId", saved.getId().toString());
        payload.put("code", saved.getCode());
        if (u != null) { payload.put("email", u.getEmail()); payload.put("firstName", u.getFirstName()); payload.put("lastName", u.getLastName()); }
        events.publish(Topics.ENROLLMENT_EVENTS, EventType.CERTIFICATE_ISSUED, userId.toString(), payload);
        return Optional.of(saved);
    }

    private static String newCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("JZ-");
        for (int i = 0; i < 8; i++) { if (i == 4) sb.append('-'); sb.append(alphabet.charAt(RND.nextInt(alphabet.length()))); }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> ofUser(UUID userId) {
        return certificates.findByUserIdOrderByIssuedAtDesc(userId).stream().map(this::toMap).toList();
    }

    @Transactional(readOnly = true)
    public Certificate get(UUID id, UUID requesterId, String role) {
        Certificate c = certificates.findById(id).orElseThrow(() -> new NotFoundException("Certificate not found: " + id));
        boolean staff = role != null && !role.equals("LEARNER");
        if (!staff && !c.getUserId().equals(requesterId)) throw new ForbiddenException("This certificate belongs to another user");
        return c;
    }

    public Map<String, Object> toMap(Certificate c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("code", c.getCode()); m.put("issuedAt", c.getIssuedAt());
        m.put("courseId", c.getCourseId());
        m.put("courseName", courses.findById(c.getCourseId()).map(Course::getName).orElse("Deleted course"));
        m.put("templateId", c.getTemplateId());
        return m;
    }

    // ---------- rendering ----------

    /** HTML выданного сертификата. */
    @Transactional(readOnly = true)
    public String render(Certificate c) {
        CertificateTemplate t = c.getTemplateId() == null ? null : templates.findById(c.getTemplateId()).orElse(null);
        if (t == null) t = templates.findFirstByIsDefaultTrue().orElseGet(CertificateTemplate::new);
        String user = users.getUser(c.getUserId()).map(u -> (u.getFirstName() + " " + u.getLastName()).trim()).orElse("Unknown user");
        String course = courses.findById(c.getCourseId()).map(Course::getName).orElse("Deleted course");
        return html(t, user, course, c.getIssuedAt(), c.getCode());
    }

    /** Предпросмотр шаблона с демонстрационными данными (кнопка Preview). */
    @Transactional(readOnly = true)
    public String preview(UUID templateId) {
        return html(template(templateId), "Aida Learner", "Java Basics", Instant.now(), "JZ-PREV-IEW1");
    }

    private static String html(CertificateTemplate t, String user, String course, Instant date, String code) {
        String when = DATE.format(date.atOffset(ZoneOffset.UTC));
        String body = esc(t.getBody()).replace("{user}", "<b>" + esc(user) + "</b>").replace("{course}", "<b>" + esc(course) + "</b>")
                .replace("{date}", esc(when)).replace("{code}", esc(code));
        String bg = BACKGROUNDS.getOrDefault(t.getBackground(), BACKGROUNDS.get("classic"));
        return """
                <!doctype html><html><head><meta charset="utf-8"><title>%s</title>
                <style>
                  body{margin:0;background:#e9ebef;font-family:Georgia,'Times New Roman',serif;color:#222}
                  .sheet{width:960px;height:680px;margin:30px auto;box-sizing:border-box;padding:70px 90px;text-align:center;%s}
                  h1{font-size:44px;letter-spacing:4px;text-transform:uppercase;margin:40px 0 20px}
                  p.body{font-size:22px;line-height:1.6;margin:30px 40px}
                  .sig{margin-top:60px;display:flex;justify-content:space-between;font-size:16px;color:#555}
                  .sig span{border-top:1px solid #888;padding-top:8px;min-width:220px}
                  .code{margin-top:30px;font-family:monospace;font-size:13px;color:#777}
                  .print{position:fixed;top:12px;right:12px;font:14px sans-serif}
                  @media print{body{background:#fff}.sheet{margin:0}.print{display:none}}
                </style></head><body>
                <button class="print" onclick="window.print()">Print / Save as PDF</button>
                <div class="sheet">
                  <div style="font-size:14px;letter-spacing:6px;color:#777">JAZZLMS</div>
                  <h1>%s</h1>
                  <p class="body">%s</p>
                  <div class="sig"><span>%s</span><span>Date: %s</span></div>
                  <div class="code">Certificate no. %s</div>
                </div></body></html>
                """.formatted(esc(t.getTitle()), bg, esc(t.getTitle()), body, esc(t.getSignature() == null ? "" : t.getSignature()), esc(when), esc(code));
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
