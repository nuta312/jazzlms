package kz.jazz.lms.course.domain;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Строка состава теста (test_id + question_id — составной ключ через @IdClass). */
@Entity
@Table(name = "test_questions")
@IdClass(TestQuestion.Key.class)
public class TestQuestion {
    public static class Key implements Serializable {
        private UUID testId; private UUID questionId;
        public Key() {}
        public Key(UUID testId, UUID questionId) { this.testId = testId; this.questionId = questionId; }
        @Override public boolean equals(Object o) { return o instanceof Key k && Objects.equals(testId, k.testId) && Objects.equals(questionId, k.questionId); }
        @Override public int hashCode() { return Objects.hash(testId, questionId); }
    }

    @Id @Column(name = "test_id") private UUID testId;
    @Id @Column(name = "question_id") private UUID questionId;
    @Column(nullable = false) private int position;
    @Column(nullable = false) private int weight = 1;

    public TestQuestion() {}
    public TestQuestion(UUID testId, UUID questionId, int position, int weight) { this.testId = testId; this.questionId = questionId; this.position = position; this.weight = weight; }

    public UUID getTestId() { return testId; }
    public UUID getQuestionId() { return questionId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
}
