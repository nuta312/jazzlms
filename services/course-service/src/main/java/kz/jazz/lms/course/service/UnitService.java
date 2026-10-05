package kz.jazz.lms.course.service;

import kz.jazz.lms.course.client.UserClient;
import kz.jazz.lms.course.domain.Course;
import kz.jazz.lms.course.domain.Enrollment;
import kz.jazz.lms.course.domain.Unit;
import kz.jazz.lms.course.domain.UnitProgress;
import kz.jazz.lms.course.dto.UnitDto;
import kz.jazz.lms.course.dto.UnitRequest;
import kz.jazz.lms.course.repository.CourseRepository;
import kz.jazz.lms.course.repository.EnrollmentRepository;
import kz.jazz.lms.course.repository.UnitProgressRepository;
import kz.jazz.lms.course.repository.UnitRepository;
import kz.jazz.lms.course.storage.FileStorage;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.events.Topics;
import kz.jazz.lms.grpc.user.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Уроки курса. Кто что может:
 *  - ADMIN / SUPER_ADMIN — управляют уроками любого курса;
 *  - TRAINER (Instructor) — только курсов, где он записан как INSTRUCTOR или которые создал сам;
 *  - LEARNER — смотрит активные уроки курсов, на которые записан, и отмечает прохождение.
 * Gateway уже отсёк учеников от "пишущих" запросов, а здесь — проверка на уровне конкретного курса.
 */
@Service
@Transactional
public class UnitService {
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");
    private static final Set<String> DOC_EXTENSIONS = Set.of("pdf", "ppt", "pptx", "doc", "docx", "odp", "xls", "xlsx");

    private final UnitRepository units;
    private final UnitProgressRepository progress;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final FileStorage storage;
    private final CourseService courseService;
    private final CourseEventPublisher events;
    private final UserClient users;

    public UnitService(UnitRepository units, UnitProgressRepository progress, CourseRepository courses,
                       EnrollmentRepository enrollments, FileStorage storage, CourseService courseService,
                       CourseEventPublisher events, UserClient users) {
        this.units = units;
        this.progress = progress;
        this.courses = courses;
        this.enrollments = enrollments;
        this.storage = storage;
        this.courseService = courseService;
        this.events = events;
        this.users = users;
    }

    // ---------- чтение ----------

    @Transactional(readOnly = true)
    public List<UnitDto> list(UUID courseId, UUID userId, String role) {
        Course course = course(courseId);
        boolean manage = canManage(course, userId, role);
        requireView(course, userId, manage);

        List<Unit> list = units.findByCourseIdOrderByPositionAsc(courseId).stream()
                .filter(u -> manage || u.isActive())
                .toList();
        Map<UUID, UnitProgress> mine = progress.findByUserIdAndUnitIdIn(userId, list.stream().map(Unit::getId).toList())
                .stream().collect(Collectors.toMap(UnitProgress::getUnitId, Function.identity()));
        // Rules & Path: при sequential ученику доступен урок только если все предыдущие активные пройдены
        boolean prevDone = true;
        List<UnitDto> out = new ArrayList<>();
        for (Unit u : list) {
            UnitProgress p = mine.get(u.getId());
            boolean locked = !manage && course.isSequential() && !prevDone;
            out.add(toDto(u, manage, false, p, locked));
            if (u.isActive() && !(p != null && p.getCompletedAt() != null)) prevDone = false;
        }
        return out;
    }

    @Transactional(readOnly = true)
    public UnitDto get(UUID unitId, UUID userId, String role) {
        Unit u = unit(unitId);
        Course course = course(u.getCourseId());
        boolean manage = canManage(course, userId, role);
        requireView(course, userId, manage);
        if (!manage && !u.isActive()) throw new NotFoundException("Unit not found: " + unitId);
        return toDto(u, manage, true, progress.findByUnitIdAndUserId(unitId, userId).orElse(null));
    }

    /** Sequential: все активные уроки ДО этого должны быть пройдены пользователем. */
    private void requireUnlocked(Course course, Unit u, UUID userId, boolean manage) {
        if (manage || !course.isSequential()) return;
        List<UUID> before = units.findByCourseIdOrderByPositionAsc(course.getId()).stream()
                .filter(x -> x.isActive() && x.getPosition() < u.getPosition()).map(Unit::getId).toList();
        long done = progress.findByUserIdAndUnitIdIn(userId, before).stream().filter(p -> p.getCompletedAt() != null).count();
        if (done < before.size()) throw new ForbiddenException("Complete the previous units first (sequential course)");
    }

    // ---------- управление (Instructor / Admin) ----------

    public UnitDto create(UUID courseId, UnitRequest req, MultipartFile file, UUID userId, String role) {
        Course course = course(courseId);
        requireManage(course, userId, role);
        validate(req, file, true);

        Unit u = new Unit();
        u.setCourseId(courseId);
        u.setType(req.type());
        u.setPosition((int) units.countByCourseId(courseId) + 1);
        u.setCreatedBy(userId);
        apply(u, req);

        if (req.type() == Unit.Type.CONTENT || req.type() == Unit.Type.TEST) {
            u.setSourceType(null);
        } else if (req.type() == Unit.Type.VIDEO && req.sourceType() == Unit.SourceType.YOUTUBE) {
            u.setSourceType(Unit.SourceType.YOUTUBE);
        } else if (req.sourceUnitId() != null && (file == null || file.isEmpty())) {
            // "Use a document from your files": копируем объект уже загруженного урока
            Unit src = unit(req.sourceUnitId());
            if (src.getFileKey() == null) throw new BadRequestException("Selected unit has no file");
            if (!canManage(course(src.getCourseId()), userId, role)) throw new ForbiddenException("You cannot use files of that course");
            u.setSourceType(Unit.SourceType.UPLOAD);
            u.setFileKey(storage.copy(src.getFileKey(), courseId));
            u.setFileName(src.getFileName()); u.setFileContentType(src.getFileContentType()); u.setFileSize(src.getFileSize());
        } else {
            u.setSourceType(Unit.SourceType.UPLOAD);
            u.setFileKey(storage.upload(courseId, file));          // байты -> MinIO
            u.setFileName(file.getOriginalFilename());
            u.setFileContentType(file.getContentType());
            u.setFileSize(file.getSize());
        }
        Unit saved = units.save(u);

        UserResponse author = users.getUser(userId).orElse(null);  // gRPC: имя автора для ленты событий
        Map<String, String> payload = new HashMap<>(Map.of(
                "courseId", courseId.toString(), "courseName", course.getName(),
                "unitId", saved.getId().toString(), "unitName", saved.getName(),
                "unitType", saved.getType().name(), "userId", userId.toString()));
        if (author != null) {
            payload.put("firstName", author.getFirstName());
            payload.put("lastName", author.getLastName());
            payload.put("email", author.getEmail());
        }
        events.publish(Topics.COURSE_EVENTS, EventType.UNIT_ADDED, courseId.toString(), payload);
        return toDto(saved, true, true, null);
    }

    public UnitDto update(UUID unitId, UnitRequest req, UUID userId, String role) {
        Unit u = unit(unitId);
        requireManage(course(u.getCourseId()), userId, role);
        if (req.type() != u.getType()) throw new BadRequestException("Unit type cannot be changed");
        validate(req, null, false);
        apply(u, req);
        return toDto(units.save(u), true, true, null);
    }

    public void delete(UUID unitId, UUID userId, String role) {
        Unit u = unit(unitId);
        requireManage(course(u.getCourseId()), userId, role);
        storage.delete(u.getFileKey());
        units.delete(u);
    }

    /** Поменять урок местами с соседним (direction: -1 вверх, +1 вниз). */
    public void move(UUID unitId, int direction, UUID userId, String role) {
        Unit u = unit(unitId);
        requireManage(course(u.getCourseId()), userId, role);
        List<Unit> list = units.findByCourseIdOrderByPositionAsc(u.getCourseId());
        int i = list.indexOf(list.stream().filter(x -> x.getId().equals(unitId)).findFirst().orElseThrow());
        int j = i + Integer.signum(direction);
        if (j < 0 || j >= list.size()) return;
        Collections.swap(list, i, j);
        for (int k = 0; k < list.size(); k++) list.get(k).setPosition(k + 1);
        units.saveAll(list);
    }

    // ---------- прохождение (Learner) ----------

    /** Ученик открыл урок: запоминаем время старта (нужно для "After a period of time"). */
    public UnitDto start(UUID unitId, UUID userId, String role) {
        Unit u = unit(unitId);
        Course course = course(u.getCourseId());
        boolean manage = canManage(course, userId, role);
        requireView(course, userId, manage);
        requireUnlocked(course, u, userId, manage);
        UnitProgress p = progress.findByUnitIdAndUserId(unitId, userId).orElseGet(() -> {
            UnitProgress n = new UnitProgress();
            n.setUnitId(unitId);
            n.setUserId(userId);
            return progress.save(n);
        });
        return toDto(u, manage, true, p);
    }

    public UnitDto complete(UUID unitId, String answer, UUID userId, String role) {
        Unit u = unit(unitId);
        Course course = course(u.getCourseId());
        boolean manage = canManage(course, userId, role);
        requireView(course, userId, manage);

        UnitProgress p = progress.findByUnitIdAndUserId(unitId, userId)
                .orElseThrow(() -> new BadRequestException("Open the unit first"));
        if (p.getCompletedAt() != null) return toDto(u, manage, true, p);
        if (u.getType() == Unit.Type.TEST) throw new BadRequestException("A test is completed by passing it, not by this button");

        switch (u.getCompletionType()) {
            case QUESTION -> {
                if (answer == null || !answer.trim().equalsIgnoreCase(u.getAnswer().trim()))
                    throw new BadRequestException("Wrong answer, try again");
            }
            case TIME -> {
                long elapsed = Duration.between(p.getStartedAt(), Instant.now()).toSeconds();
                if (elapsed < u.getTimeLimitSeconds())
                    throw new BadRequestException("Too early: " + (u.getTimeLimitSeconds() - elapsed) + " more second(s)");
            }
            case CHECKBOX -> { /* достаточно нажать кнопку */ }
        }
        markCompleted(u, course, p, userId);
        return toDto(u, manage, true, p);
    }

    /**
     * Урок пройден: отметка в unit_progress, пересчёт прогресса курса и событие UNIT_COMPLETED.
     * Вызывается и для обычных уроков (кнопка Complete), и из TestService, когда тест сдан.
     */
    public void markCompleted(Unit u, Course course, UnitProgress p, UUID userId) {
        if (p.getCompletedAt() != null) return;
        p.setCompletedAt(Instant.now());
        progress.save(p);

        // Прогресс курса = доля пройденных активных уроков. При 100% CourseService сам отправит COURSE_COMPLETED.
        List<UUID> activeIds = units.findByCourseIdAndActiveTrue(course.getId()).stream().map(Unit::getId).toList();
        long done = progress.findByUserIdAndUnitIdIn(userId, activeIds).stream().filter(x -> x.getCompletedAt() != null).count();
        int percent = activeIds.isEmpty() ? 0 : (int) Math.round(done * 100.0 / activeIds.size());
        courseService.setProgressFor(course.getId(), userId, percent);

        events.publish(Topics.ENROLLMENT_EVENTS, EventType.UNIT_COMPLETED, userId.toString(), Map.of(
                "courseId", course.getId().toString(), "courseName", course.getName(),
                "unitId", u.getId().toString(), "unitName", u.getName(),
                "userId", userId.toString(), "progress", String.valueOf(percent)));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> filesOfUser(UUID userId) {
        return units.findByCreatedByAndFileKeyIsNotNullOrderByCreatedAtDesc(userId).stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("unitId", u.getId()); m.put("unitName", u.getName()); m.put("courseId", u.getCourseId());
            m.put("fileName", u.getFileName()); m.put("contentType", u.getFileContentType()); m.put("size", u.getFileSize());
            m.put("uploadedAt", u.getCreatedAt()); m.put("type", u.getType().name());
            return m;
        }).toList();
    }

    // ---------- helpers ----------

    boolean canManage(Course course, UUID userId, String role) {
        if (ADMIN_ROLES.contains(role)) return true;
        if (!"TRAINER".equals(role)) return false;
        if (userId.equals(course.getCreatedBy())) return true;
        return enrollments.findByCourseIdAndUserId(course.getId(), userId)
                .map(e -> e.getRole() == Enrollment.Role.INSTRUCTOR).orElse(false);
    }

    private void requireManage(Course course, UUID userId, String role) {
        if (!canManage(course, userId, role))
            throw new ForbiddenException("You are not an instructor of this course");
    }

    private void requireView(Course course, UUID userId, boolean manage) {
        if (manage) return;
        if (enrollments.findByCourseIdAndUserId(course.getId(), userId).isEmpty())
            throw new ForbiddenException("You are not enrolled in this course");
    }

    private void validate(UnitRequest req, MultipartFile file, boolean creating) {
        Unit.CompletionType ct = req.completionType() == null ? Unit.CompletionType.CHECKBOX : req.completionType();
        if (ct == Unit.CompletionType.QUESTION && (blank(req.question()) || blank(req.answer())))
            throw new BadRequestException("Question and answer are required for 'With a question'");
        if (ct == Unit.CompletionType.TIME && req.timeLimitSeconds() == null)
            throw new BadRequestException("Time limit is required for 'After a period of time'");

        switch (req.type()) {
            case CONTENT -> { if (blank(req.textContent())) throw new BadRequestException("Content text is required"); }
            case TEST -> throw new BadRequestException("Create tests via POST /api/courses/{id}/tests");
            case VIDEO -> {
                if (req.sourceType() == Unit.SourceType.YOUTUBE) {
                    if (YouTube.videoId(req.youtubeUrl()).isEmpty()) throw new BadRequestException("Not a valid YouTube link");
                } else if (creating && req.sourceUnitId() == null) {
                    requireFile(file);
                    String ctType = file.getContentType();
                    if (ctType == null || !ctType.startsWith("video/")) throw new BadRequestException("File must be a video (mp4, webm, ...)");
                }
            }
            case PRESENTATION -> {
                if (creating && req.sourceUnitId() == null) {
                    requireFile(file);
                    String n = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
                    String ext = n.contains(".") ? n.substring(n.lastIndexOf('.') + 1) : "";
                    if (!DOC_EXTENSIONS.contains(ext)) throw new BadRequestException("Allowed files: " + DOC_EXTENSIONS);
                }
            }
        }
    }

    private static void requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File is required");
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    private void apply(Unit u, UnitRequest req) {
        u.setName(req.name().trim());
        if (req.active() != null) u.setActive(req.active());
        Unit.CompletionType ct = req.completionType() == null ? Unit.CompletionType.CHECKBOX : req.completionType();
        u.setCompletionType(ct);
        u.setTimeLimitSeconds(ct == Unit.CompletionType.TIME ? req.timeLimitSeconds() : null);
        u.setQuestion(ct == Unit.CompletionType.QUESTION ? req.question() : null);
        u.setAnswer(ct == Unit.CompletionType.QUESTION ? req.answer() : null);
        if (req.autoplay() != null) u.setAutoplay(req.autoplay());
        if (req.showSpeed() != null) u.setShowSpeed(req.showSpeed());
        u.setDescription(req.description());
        if (req.type() == Unit.Type.CONTENT) u.setTextContent(req.textContent());
        if (req.type() == Unit.Type.VIDEO && req.sourceType() == Unit.SourceType.YOUTUBE) u.setYoutubeUrl(req.youtubeUrl().trim());
    }

    private UnitDto toDto(Unit u, boolean manage, boolean withFileUrl, UnitProgress p) { return toDto(u, manage, withFileUrl, p, false); }

    private UnitDto toDto(Unit u, boolean manage, boolean withFileUrl, UnitProgress p, boolean locked) {
        String fileUrl = withFileUrl && u.getFileKey() != null ? storage.presignedUrl(u.getFileKey()) : null;
        return new UnitDto(u.getId(), u.getCourseId(), u.getType(), u.getName(), u.getPosition(), u.isActive(),
                u.getCompletionType(), u.getTimeLimitSeconds(), u.getQuestion(), manage ? u.getAnswer() : null,
                u.getSourceType(), u.getYoutubeUrl(), YouTube.embedUrl(u.getYoutubeUrl()),
                withFileUrl ? u.getTextContent() : null,
                u.getFileName(), u.getFileContentType(), u.getFileSize(), fileUrl, u.getCreatedAt(),
                p != null && p.getCompletedAt() != null, p == null ? null : p.getStartedAt(),
                u.isAutoplay(), u.isShowSpeed(), withFileUrl ? u.getDescription() : null, locked);
    }

    Unit unit(UUID id) { return units.findById(id).orElseThrow(() -> new NotFoundException("Unit not found: " + id)); }

    private Course course(UUID id) { return courses.findById(id).filter(c -> c.getDeletedAt() == null).orElseThrow(() -> new NotFoundException("Course not found: " + id)); }
}
