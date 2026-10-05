package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Вопрос из банка вопросов курса. Поле data — JSONB, структура зависит от типа:
 *  MULTIPLE_CHOICE  {answers:[{text, correct}]}
 *  FILL_GAP         {} — пропуски задаются в тексте: "The quick [fox] jumps over the [lazy|slow] dog" (первый вариант верный)
 *  ORDERING         {items:[... в правильном порядке]}
 *  DRAG_DROP        {pairs:[{left, right}]}
 *  FREE_TEXT        {threshold: 1, options:[{mode: contains|not_contains|equals, word: "fast|quick", points: 1}]}
 *  RANDOMIZED       {pool:[questionId, ...]} — при каждом показе берётся случайный вопрос из пула
 * Hibernate 6 умеет JSON: @JdbcTypeCode(SqlTypes.JSON) сериализует Map через Jackson в jsonb PostgreSQL.
 */
@Entity
@Table(name = "questions")
public class Question {
    public enum Type { MULTIPLE_CHOICE, FILL_GAP, ORDERING, DRAG_DROP, FREE_TEXT, RANDOMIZED }

    @Id private UUID id;
    @Column(name = "course_id", nullable = false) private UUID courseId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type type;
    @Column(nullable = false, columnDefinition = "TEXT") private String text;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Object> data = new HashMap<>();
    @Column(columnDefinition = "TEXT") private String feedback;
    private String tags;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
