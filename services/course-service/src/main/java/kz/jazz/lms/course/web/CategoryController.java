package kz.jazz.lms.course.web;

import jakarta.validation.Valid;
import kz.jazz.lms.course.dto.CategoryDto;
import kz.jazz.lms.course.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public List<CategoryDto> list() { return service.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CategoryDto dto) { return service.create(dto); }

    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable UUID id, @Valid @RequestBody CategoryDto dto) { return service.update(id, dto); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { service.delete(id); }
}
