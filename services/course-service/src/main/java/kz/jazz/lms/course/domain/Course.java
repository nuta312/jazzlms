package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(length = 5000)
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    private BigDecimal price;
    private Integer capacity;
    private String level;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "hidden_from_catalog", nullable = false)
    private boolean hiddenFromCatalog = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** Rules & Path: уроки открываются строго по порядку. */
    @Column(nullable = false)
    private boolean sequential = false;

    /** Шаблон сертификата, который выдаётся при завершении курса (NULL = без сертификата). */
    @Column(name = "certificate_template_id")
    private UUID certificateTemplateId;

    /** null = курс жив; дата = мягко удалён (скрыт везде, но восстановим). */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isHiddenFromCatalog() { return hiddenFromCatalog; }
    public void setHiddenFromCatalog(boolean hiddenFromCatalog) { this.hiddenFromCatalog = hiddenFromCatalog; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public boolean isSequential() { return sequential; }
    public void setSequential(boolean sequential) { this.sequential = sequential; }
    public UUID getCertificateTemplateId() { return certificateTemplateId; }
    public void setCertificateTemplateId(UUID certificateTemplateId) { this.certificateTemplateId = certificateTemplateId; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
