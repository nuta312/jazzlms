package kz.jazz.lms.course.web;

import jakarta.validation.Valid;
import kz.jazz.lms.course.dto.QuestionRequest;
import kz.jazz.lms.course.dto.TestRequest;
import kz.jazz.lms.course.service.TestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Банк вопросов и тесты.
 *   GET/POST /api/courses/{id}/questions           банк вопросов курса (staff)
 *   POST     /api/courses/{id}/questions/import     импорт AIKEN (?dryRun=true — только предпросмотр)
 *   GET/PUT/DELETE /api/questions/{id}
 *   POST     /api/courses/{id}/tests                Add Test (создаёт урок типа TEST)
 *   GET/PUT  /api/units/{id}/test                   настройки и состав теста
 *   POST     /api/units/{id}/test/attempts          старт попытки (learner)
 *   POST     /api/units/{id}/test/attempts/{a}/submit
 *   GET      /api/units/{id}/test/attempts[/{a}]
 */
@RestController
public class TestController {
    private final TestService service;

    public TestController(TestService service) { this.service = service; }

    @GetMapping("/api/courses/{courseId}/questions")
    public List<Map<String, Object>> questions(@PathVariable UUID courseId, @RequestParam(defaultValue = "false") boolean all,
                                               @RequestParam(required = false) String search,
                                               @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.questionsOf(courseId, all, search, userId, role);
    }

    @PostMapping("/api/courses/{courseId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createQuestion(@PathVariable UUID courseId, @Valid @RequestBody QuestionRequest req,
                                              @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.createQuestion(courseId, req, userId, role);
    }

    @PostMapping("/api/courses/{courseId}/questions/import")
    public List<Map<String, Object>> importQuestions(@PathVariable UUID courseId, @RequestBody Map<String, String> body,
                                                     @RequestParam(defaultValue = "false") boolean dryRun,
                                                     @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.importAiken(courseId, body.get("data"), dryRun, userId, role);
    }

    @GetMapping("/api/questions/{id}")
    public Map<String, Object> question(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.question(id, userId, role);
    }

    @PutMapping("/api/questions/{id}")
    public Map<String, Object> updateQuestion(@PathVariable UUID id, @Valid @RequestBody QuestionRequest req,
                                              @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.updateQuestion(id, req, userId, role);
    }

    @DeleteMapping("/api/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        service.deleteQuestion(id, userId, role);
    }

    @PostMapping("/api/courses/{courseId}/tests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createTest(@PathVariable UUID courseId, @Valid @RequestBody TestRequest req,
                                          @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.createTest(courseId, req, userId, role);
    }

    @GetMapping("/api/units/{id}/test")
    public Map<String, Object> test(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.testView(id, userId, role);
    }

    @PutMapping("/api/units/{id}/test")
    public Map<String, Object> updateTest(@PathVariable UUID id, @Valid @RequestBody TestRequest req,
                                          @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.updateTest(id, req, userId, role);
    }

    @PostMapping("/api/units/{id}/test/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> start(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.startAttempt(id, userId, role);
    }

    @PostMapping("/api/units/{id}/test/attempts/{attemptId}/submit")
    public Map<String, Object> submit(@PathVariable UUID id, @PathVariable UUID attemptId, @RequestBody(required = false) Map<String, Object> body,
                                      @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        @SuppressWarnings("unchecked")
        Map<String, Object> answers = body == null ? null : (Map<String, Object>) body.get("answers");
        return service.submit(id, attemptId, answers, userId, role);
    }

    @GetMapping("/api/units/{id}/test/attempts")
    public List<Map<String, Object>> attempts(@PathVariable UUID id, @RequestParam(required = false) UUID userId,
                                              @RequestHeader("X-User-Id") UUID me, @RequestHeader("X-User-Role") String role) {
        return service.attemptsOf(id, userId, me, role);
    }

    @GetMapping("/api/units/{id}/test/attempts/{attemptId}")
    public Map<String, Object> attempt(@PathVariable UUID id, @PathVariable UUID attemptId,
                                       @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.attempt(id, attemptId, userId, role);
    }
}
