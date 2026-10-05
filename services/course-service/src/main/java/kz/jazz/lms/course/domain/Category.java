package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Самоссылка: категория может иметь родителя (дерево категорий). */
    @Column(name = "parent_id")
    private UUID parentId;

    private BigDecimal price;

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
