package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Попытка прохождения теста. Вопросы сохраняются в том виде, в каком их показали ученику (важно при перемешивании). */
@Entity
@Table(name = "test_attempts")
public class TestAttempt {
    @Id private UUID id;
    @Column(name = "test_id", nullable = false) private UUID testId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "started_at", nullable = false) private Instant startedAt = Instant.now();
    @Column(name = "submitted_at") private Instant submittedAt;
    private Integer score;
    private Boolean passed;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<Map<String, Object>> questions;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private Map<String, Object> answers;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private List<Map<String, Object>> results;

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getTestId() { return testId; }
    public void setTestId(UUID testId) { this.testId = testId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    public List<Map<String, Object>> getQuestions() { return questions; }
    public void setQuestions(List<Map<String, Object>> questions) { this.questions = questions; }
    public Map<String, Object> getAnswers() { return answers; }
    public void setAnswers(Map<String, Object> answers) { this.answers = answers; }
    public List<Map<String, Object>> getResults() { return results; }
    public void setResults(List<Map<String, Object>> results) { this.results = results; }
}
