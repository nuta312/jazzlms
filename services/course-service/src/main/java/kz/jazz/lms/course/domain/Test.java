package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.util.UUID;

/** Настройки теста ("Test options"). Первичный ключ = id урока типа TEST. */
@Entity
@Table(name = "tests")
public class Test {
    public enum Repetitions { ALWAYS, IF_NOT_PASSED, NEVER }
    public enum ShowCorrect { ALWAYS, WHEN_PASSED, NEVER }

    @Id @Column(name = "unit_id") private UUID unitId;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(name = "pass_score", nullable = false) private int passScore = 50;
    @Column(name = "shuffle_questions", nullable = false) private boolean shuffleQuestions;
    @Column(name = "shuffle_answers", nullable = false) private boolean shuffleAnswers;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Repetitions repetitions = Repetitions.IF_NOT_PASSED;
    @Column(name = "max_attempts") private Integer maxAttempts;
    @Enumerated(EnumType.STRING) @Column(name = "show_correct_answers", nullable = false) private ShowCorrect showCorrectAnswers = ShowCorrect.WHEN_PASSED;
    @Column(name = "show_given_answers", nullable = false) private boolean showGivenAnswers = true;
    @Column(name = "show_labels", nullable = false) private boolean showLabels = true;
    @Column(name = "show_score", nullable = false) private boolean showScore = true;
    private String description;
    @Column(name = "message_passed", columnDefinition = "TEXT") private String messagePassed;
    @Column(name = "message_failed", columnDefinition = "TEXT") private String messageFailed;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getPassScore() { return passScore; }
    public void setPassScore(int passScore) { this.passScore = passScore; }
    public boolean isShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }
    public boolean isShuffleAnswers() { return shuffleAnswers; }
    public void setShuffleAnswers(boolean shuffleAnswers) { this.shuffleAnswers = shuffleAnswers; }
    public Repetitions getRepetitions() { return repetitions; }
    public void setRepetitions(Repetitions repetitions) { this.repetitions = repetitions; }
    public Integer getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(Integer maxAttempts) { this.maxAttempts = maxAttempts; }
    public ShowCorrect getShowCorrectAnswers() { return showCorrectAnswers; }
    public void setShowCorrectAnswers(ShowCorrect showCorrectAnswers) { this.showCorrectAnswers = showCorrectAnswers; }
    public boolean isShowGivenAnswers() { return showGivenAnswers; }
    public void setShowGivenAnswers(boolean showGivenAnswers) { this.showGivenAnswers = showGivenAnswers; }
    public boolean isShowLabels() { return showLabels; }
    public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }
    public boolean isShowScore() { return showScore; }
    public void setShowScore(boolean showScore) { this.showScore = showScore; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getMessagePassed() { return messagePassed; }
    public void setMessagePassed(String messagePassed) { this.messagePassed = messagePassed; }
    public String getMessageFailed() { return messageFailed; }
    public void setMessageFailed(String messageFailed) { this.messageFailed = messageFailed; }
}
