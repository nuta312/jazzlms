package kz.jazz.lms.course.service;

import kz.jazz.lms.course.client.UserClient;
import kz.jazz.lms.course.domain.Course;
import kz.jazz.lms.course.domain.Enrollment;
import kz.jazz.lms.course.domain.Unit;
import kz.jazz.lms.course.domain.UnitProgress;
import kz.jazz.lms.course.repository.*;
import kz.jazz.lms.grpc.user.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Отчёты по курсу (Course -> Reports): Overview, Users, Unit matrix.
 * Все данные — из своей БД (enrollments, units, unit_progress) + имена по gRPC одним batch-вызовом.
 */
@Service
@Transactional(readOnly = true)
public class CourseReportService {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UnitRepository units;
    private final UnitProgressRepository progress;
    private final UserClient users;
    private final TestAttemptRepository attempts;

    public CourseReportService(CourseRepository courses, EnrollmentRepository enrollments, UnitRepository units,
                               UnitProgressRepository progress, UserClient users, TestAttemptRepository attempts) {
        this.courses = courses; this.enrollments = enrollments; this.units = units; this.progress = progress; this.users = users;
        this.attempts = attempts;
    }

    public Map<String, Object> overview(UUID courseId) {
        course(courseId);
        List<Enrollment> all = enrollments.findByCourseId(courseId);
        long learners = all.stream().filter(e -> e.getRole() == Enrollment.Role.LEARNER).count();
        long completed = all.stream().filter(e -> e.getRole() == Enrollment.Role.LEARNER && e.getCompletedAt() != null).count();
        long inProgress = all.stream().filter(e -> e.getRole() == Enrollment.Role.LEARNER && e.getCompletedAt() == null && e.getProgress() > 0).count();
        long instructors = all.size() - learners;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assignedLearners", learners);
        m.put("completedLearners", completed);
        m.put("learnersInProgress", inProgress);
        m.put("notStarted", learners - completed - inProgress);
        m.put("instructors", instructors);
        m.put("trainingSeconds", (long) progress.trainingSecondsOfCourse(courseId));
        m.put("units", units.countByCourseId(courseId));
        return m;
    }

    /** Вкладка Users: прогресс, дата завершения и время каждого ученика в этом курсе. */
    public List<Map<String, Object>> usersReport(UUID courseId) {
        course(courseId);
        List<Enrollment> all = enrollments.findByCourseId(courseId);
        Map<String, UserResponse> names = users.getUsers(all.stream().map(Enrollment::getUserId).toList());
        Map<UUID, Long> seconds = new HashMap<>();
        for (Object[] row : progress.trainingSecondsPerUser(courseId)) seconds.put((UUID) row[0], ((Number) row[1]).longValue());
        Map<UUID, Integer> scores = new HashMap<>();     // Score = средний лучший балл по тестам курса
        for (Object[] row : attempts.bestScorePerUser(courseId)) scores.put((UUID) row[0], (int) Math.round(((Number) row[1]).doubleValue()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Enrollment e : all) {
            UserResponse u = names.get(e.getUserId().toString());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("enrollmentId", e.getId()); m.put("userId", e.getUserId());
            m.put("name", u == null ? "(unknown)" : u.getFirstName() + " " + u.getLastName());
            m.put("email", u == null ? null : u.getEmail());
            m.put("role", e.getRole()); m.put("progress", e.getProgress());
            m.put("enrolledAt", e.getEnrolledAt()); m.put("completedAt", e.getCompletedAt());
            m.put("seconds", seconds.getOrDefault(e.getUserId(), 0L));
            m.put("score", scores.get(e.getUserId()));
            out.add(m);
        }
        out.sort(Comparator.comparing(m -> String.valueOf(m.get("name"))));
        return out;
    }

    /** Unit matrix: ученики × уроки. Ячейка: COMPLETED / STARTED / null и секунды в уроке. */
    public Map<String, Object> matrix(UUID courseId) {
        course(courseId);
        List<Unit> unitList = units.findByCourseIdOrderByPositionAsc(courseId);
        List<Enrollment> learners = enrollments.findByCourseId(courseId).stream().filter(e -> e.getRole() == Enrollment.Role.LEARNER).toList();
        Map<String, UserResponse> names = users.getUsers(learners.stream().map(Enrollment::getUserId).toList());
        List<UUID> unitIds = unitList.stream().map(Unit::getId).toList();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Enrollment e : learners) {
            Map<String, Object> cells = new HashMap<>();
            for (UnitProgress p : progress.findByUserIdAndUnitIdIn(e.getUserId(), unitIds)) {
                long sec = p.getCompletedAt() == null ? 0 : java.time.Duration.between(p.getStartedAt(), p.getCompletedAt()).toSeconds();
                cells.put(p.getUnitId().toString(), Map.of("status", p.getCompletedAt() != null ? "COMPLETED" : "STARTED", "seconds", sec));
            }
            UserResponse u = names.get(e.getUserId().toString());
            rows.add(Map.of("userId", e.getUserId(), "name", u == null ? "(unknown)" : u.getFirstName() + " " + u.getLastName(), "cells", cells));
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("name"))));
        List<Map<String, Object>> unitCols = unitList.stream().map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getName(), "type", u.getType(), "active", u.isActive())).toList();
        return Map.of("units", unitCols, "rows", rows);
    }

    /** Course -> Files: файлы всех уроков курса. */
    public List<Map<String, Object>> files(UUID courseId) {
        course(courseId);
        return units.findByCourseIdOrderByPositionAsc(courseId).stream().filter(u -> u.getFileKey() != null).map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("unitId", u.getId()); m.put("unitName", u.getName()); m.put("fileName", u.getFileName());
            m.put("contentType", u.getFileContentType()); m.put("size", u.getFileSize()); m.put("uploadedAt", u.getCreatedAt()); m.put("type", u.getType());
            return m;
        }).toList();
    }

    private Course course(UUID id) {
        return courses.findById(id).filter(c -> c.getDeletedAt() == null).orElseThrow(() -> new NotFoundException("Course not found: " + id));
    }
}
