package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Урок курса. Файл (видео/презентация) хранится в MinIO, здесь — только fileKey. */
@Entity
@Table(name = "units")
public class Unit {
    public enum Type { VIDEO, PRESENTATION, CONTENT, TEST }   // TEST: настройки и вопросы в таблицах tests / test_questions
    public enum CompletionType { CHECKBOX, QUESTION, TIME }
    public enum SourceType { YOUTUBE, UPLOAD }

    @Id
    private UUID id;
    @Column(name = "course_id", nullable = false)
    private UUID courseId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Column(nullable = false, length = 80)
    private String name;
    @Column(nullable = false)
    private int position;
    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_type", nullable = false)
    private CompletionType completionType = CompletionType.CHECKBOX;
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;
    private String question;
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;
    @Column(name = "youtube_url")
    private String youtubeUrl;
    @Column(name = "text_content", columnDefinition = "TEXT")
    private String textContent;
    @Column(name = "file_key")
    private String fileKey;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_content_type")
    private String fileContentType;
    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false)
    private boolean autoplay = false;
    @Column(name = "show_speed", nullable = false)
    private boolean showSpeed = true;
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    void prePersist() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCourseId() { return courseId; }
    public void setCourseId(UUID courseId) { this.courseId = courseId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public CompletionType getCompletionType() { return completionType; }
    public void setCompletionType(CompletionType completionType) { this.completionType = completionType; }
    public Integer getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(Integer timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileContentType() { return fileContentType; }
    public void setFileContentType(String fileContentType) { this.fileContentType = fileContentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public boolean isAutoplay() { return autoplay; }
    public void setAutoplay(boolean autoplay) { this.autoplay = autoplay; }
    public boolean isShowSpeed() { return showSpeed; }
    public void setShowSpeed(boolean showSpeed) { this.showSpeed = showSpeed; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
