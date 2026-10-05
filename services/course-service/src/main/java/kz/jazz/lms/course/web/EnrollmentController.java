package kz.jazz.lms.course.web;

import kz.jazz.lms.course.domain.Enrollment;
import kz.jazz.lms.course.dto.MyEnrollmentDto;
import kz.jazz.lms.course.dto.EnrollmentDto;
import kz.jazz.lms.course.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {
    private final CourseService service;

    public EnrollmentController(CourseService service) { this.service = service; }

    /** Курсы текущего пользователя ("My courses"). */
    @GetMapping("/my")
    public List<MyEnrollmentDto> my(@RequestHeader("X-User-Id") UUID userId,
                                    @RequestParam(required = false) Enrollment.Role role) {
        return service.coursesOfUser(userId, role);
    }

    @GetMapping("/my/stats")
    public Map<String, Long> myStats(@RequestHeader("X-User-Id") UUID userId) { return service.myStats(userId); }

    /** Курсы и прогресс конкретного пользователя — для отчёта Users -> Reports (только staff, правило в gateway). */
    @GetMapping("/user/{userId}")
    public List<MyEnrollmentDto> ofUser(@PathVariable UUID userId) { return service.coursesOfUser(userId, null); }

    @PatchMapping("/{id}/progress")
    public EnrollmentDto progress(@PathVariable UUID id, @RequestBody Map<String, Integer> body) {
        return service.updateProgress(id, body.getOrDefault("progress", 0));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unenroll(@PathVariable UUID id) { service.unenroll(id); }
}
