package kz.jazz.lms.course.service;

import kz.jazz.lms.course.domain.Category;
import kz.jazz.lms.course.dto.CategoryDto;
import kz.jazz.lms.course.repository.CategoryRepository;
import kz.jazz.lms.course.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository repo;
    private final CourseRepository courses;

    public CategoryService(CategoryRepository repo, CourseRepository courses) {
        this.repo = repo;
        this.courses = courses;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> findAll() {
        return repo.findAll().stream().map(CategoryDto::from).toList();
    }

    public CategoryDto create(CategoryDto dto) {
        if (repo.existsByNameIgnoreCase(dto.name())) throw new ConflictException("Category already exists: " + dto.name());
        if (dto.parentId() != null && !repo.existsById(dto.parentId()))
            throw new NotFoundException("Parent category not found: " + dto.parentId());
        Category c = new Category();
        c.setName(dto.name());
        c.setParentId(dto.parentId());
        c.setPrice(dto.price());
        return CategoryDto.from(repo.save(c));
    }

    public CategoryDto update(UUID id, CategoryDto dto) {
        Category c = repo.findById(id).orElseThrow(() -> new NotFoundException("Category not found: " + id));
        c.setName(dto.name());
        c.setParentId(dto.parentId());
        c.setPrice(dto.price());
        return CategoryDto.from(repo.save(c));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) throw new NotFoundException("Category not found: " + id);
        if (courses.countByCategoryId(id) > 0) throw new ConflictException("Category has courses, move them first");
        repo.deleteById(id);
    }
}
