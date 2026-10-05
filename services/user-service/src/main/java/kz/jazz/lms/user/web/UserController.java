package kz.jazz.lms.user.web;

import jakarta.validation.Valid;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.user.dto.CreateUserRequest;
import kz.jazz.lms.user.dto.UpdateUserRequest;
import kz.jazz.lms.user.dto.UserDto;
import kz.jazz.lms.user.service.UserService;
import kz.jazz.lms.user.dto.ImportResult;
import kz.jazz.lms.user.service.UserImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API: ресурс "users".
 *   GET    /api/users        список
 *   POST   /api/users        создать
 *   GET    /api/users/{id}   один
 *   PUT    /api/users/{id}   обновить
 *   DELETE /api/users/{id}   удалить
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService users;
    private final UserImportService importer;

    public UserController(UserService users, UserImportService importer) {
        this.users = users;
        this.importer = importer;
    }

    @GetMapping
    public List<UserDto> list(@RequestParam(required = false) String search,
                              @RequestParam(required = false) Boolean active) {
        return users.findAll(search, active);
    }

    @GetMapping("/stats")
    public java.util.Map<String, Long> stats() { return users.stats(); }

    /** Save as CSV: тот же запрос, но ответ — файл. */
    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestParam(required = false) String search,
                                         @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"users.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body("\uFEFF" + users.exportCsv(search, active));
    }

    /** Import user(s): multipart с CSV-файлом. Ответ — отчёт: сколько создано, какие строки пропущены и почему. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importUsers(@RequestPart("file") MultipartFile file) throws IOException {
        return importer.importCsv(file);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable UUID id) {
        return users.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest req) {
        return users.create(req, EventType.USER_CREATED);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return users.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        users.delete(id);
    }
}
