package kz.jazz.lms.course.web;

import jakarta.validation.Valid;
import kz.jazz.lms.course.dto.UnitDto;
import kz.jazz.lms.course.dto.UnitRequest;
import kz.jazz.lms.course.service.UnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Уроки. Создание — multipart/form-data из двух частей:
 *   unit = JSON (application/json) с полями формы,
 *   file = сам файл (видео или презентация), необязателен для YouTube и текстового урока.
 *
 * curl -F 'unit={"name":"Intro","type":"VIDEO","sourceType":"UPLOAD"};type=application/json' -F file=@intro.mp4 ...
 */
@RestController
public class UnitController {
    private final UnitService service;

    public UnitController(UnitService service) { this.service = service; }

    @GetMapping("/api/courses/{courseId}/units")
    public List<UnitDto> list(@PathVariable UUID courseId,
                              @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.list(courseId, userId, role);
    }

    @PostMapping(value = "/api/courses/{courseId}/units", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UnitDto create(@PathVariable UUID courseId,
                          @Valid @RequestPart("unit") UnitRequest unit,
                          @RequestPart(value = "file", required = false) MultipartFile file,
                          @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.create(courseId, unit, file, userId, role);
    }

    @GetMapping("/api/units/{id}")
    public UnitDto get(@PathVariable UUID id,
                       @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.get(id, userId, role);
    }

    @PutMapping("/api/units/{id}")
    public UnitDto update(@PathVariable UUID id, @Valid @RequestBody UnitRequest unit,
                          @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.update(id, unit, userId, role);
    }

    @DeleteMapping("/api/units/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        service.delete(id, userId, role);
    }

    @PostMapping("/api/units/{id}/move")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void move(@PathVariable UUID id, @RequestBody Map<String, Integer> body,
                     @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        service.move(id, body.getOrDefault("direction", 0), userId, role);
    }

    /** Users -> Files: файлы, загруженные пользователем в уроки (staff, правило в gateway). */
    @GetMapping("/api/units/files/{userId}")
    public List<Map<String, Object>> filesOfUser(@PathVariable UUID userId) { return service.filesOfUser(userId); }

    // --- прохождение ---

    @PostMapping("/api/units/{id}/start")
    public UnitDto start(@PathVariable UUID id,
                         @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.start(id, userId, role);
    }

    @PostMapping("/api/units/{id}/complete")
    public UnitDto complete(@PathVariable UUID id, @RequestBody(required = false) Map<String, String> body,
                            @RequestHeader("X-User-Id") UUID userId, @RequestHeader("X-User-Role") String role) {
        return service.complete(id, body == null ? null : body.get("answer"), userId, role);
    }
}
