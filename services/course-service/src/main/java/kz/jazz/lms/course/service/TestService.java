package kz.jazz.lms.course.service;

import kz.jazz.lms.course.domain.*;
import kz.jazz.lms.course.dto.QuestionRequest;
import kz.jazz.lms.course.dto.TestRequest;
import kz.jazz.lms.course.repository.*;
import kz.jazz.lms.events.EventType;
import kz.jazz.lms.events.Topics;
import kz.jazz.lms.grpc.user.UserResponse;
import kz.jazz.lms.course.client.UserClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Тесты: банк вопросов, состав и настройки теста, попытки учеников и проверка ответов.
 * Проверка — только на сервере: ученик получает вопросы БЕЗ правильных ответов, отправляет свои,
 * сервер сравнивает с банком и считает балл. Так правильные ответы нельзя подсмотреть в DevTools.
 */
@Service
@Transactional
public class TestService {
    private static final Pattern GAP = Pattern.compile("\\[([^\\]]+)]");
    private static final Set<String> ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

    private final QuestionRepository questions;
    private final TestRepository tests;
    private final TestQuestionRepository testQuestions;
    private final TestAttemptRepository attempts;
    private final UnitRepository units;
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final UnitProgressRepository progress;
    private final UnitService unitService;
    private final CourseEventPublisher events;
    private final UserClient users;

    public TestService(QuestionRepository questions, TestRepository tests, TestQuestionRepository testQuestions,
                       TestAttemptRepository attempts, UnitRepository units, CourseRepository courses,
                       EnrollmentRepository enrollments, UnitProgressRepository progress, UnitService unitService,
                       CourseEventPublisher events, UserClient users) {
        this.questions = questions; this.tests = tests; this.testQuestions = testQuestions; this.attempts = attempts;
        this.units = units; this.courses = courses; this.enrollments = enrollments; this.progress = progress;
        this.unitService = unitService; this.events = events; this.users = users;
    }

    // =====================================================================
    //  Банк вопросов (Instructor / Admin)
    // =====================================================================

    /** all = true: "Show questions from all courses" — вопросы всех курсов, которыми пользователь управляет. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> questionsOf(UUID courseId, boolean all, String search, UUID userId, String role) {
        Course course = course(courseId);
        requireManage(course, userId, role);
        List<Question> list;
        if (!all) list = questions.findByCourseIdOrderByCreatedAtDesc(courseId);
        else if (ADMIN_ROLES.contains(role)) list = questions.findAllByOrderByCreatedAtDesc();
        else list = questions.findByCourseIdInOrderByCreatedAtDesc(managedCourseIds(userId));
        String q = search == null ? "" : search.trim().toLowerCase();
        Map<UUID, String> courseNames = new HashMap<>();
        return list.stream()
                .filter(x -> q.isEmpty() || x.getText().toLowerCase().contains(q) || (x.getTags() != null && x.getTags().toLowerCase().contains(q)))
                .map(x -> questionMap(x, true, courseNames)).toList();
    }

    private List<UUID> managedCourseIds(UUID userId) {
        Set<UUID> ids = new HashSet<>();
        enrollments.findByUserId(userId).stream().filter(e -> e.getRole() == Enrollment.Role.INSTRUCTOR).forEach(e -> ids.add(e.getCourseId()));
        courses.findByCreatedByAndDeletedAtIsNull(userId).forEach(c -> ids.add(c.getId()));
        return new ArrayList<>(ids);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> question(UUID id, UUID userId, String role) {
        Question q = questionEntity(id);
        requireManage(course(q.getCourseId()), userId, role);
        return questionMap(q, true, new HashMap<>());
    }

    public Map<String, Object> createQuestion(UUID courseId, QuestionRequest req, UUID userId, String role) {
        requireManage(course(courseId), userId, role);
        Question q = new Question();
        q.setCourseId(courseId);
        q.setCreatedBy(userId);
        applyQuestion(q, req);
        return questionMap(questions.save(q), true, new HashMap<>());
    }

    public Map<String, Object> updateQuestion(UUID id, QuestionRequest req, UUID userId, String role) {
        Question q = questionEntity(id);
        requireManage(course(q.getCourseId()), userId, role);
        if (req.type() != q.getType()) throw new BadRequestException("Question type cannot be changed");
        applyQuestion(q, req);
        q.setUpdatedAt(Instant.now());
        return questionMap(questions.save(q), true, new HashMap<>());
    }

    public void deleteQuestion(UUID id, UUID userId, String role) {
        Question q = questionEntity(id);
        requireManage(course(q.getCourseId()), userId, role);
        questions.delete(q);   // из состава тестов удалится каскадом (FK ON DELETE CASCADE)
    }

    /** Валидация структуры data по типу вопроса — чтобы в банк не попал вопрос без правильного ответа. */
    private void applyQuestion(Question q, QuestionRequest req) {
        Map<String, Object> d = req.data() == null ? new HashMap<>() : new HashMap<>(req.data());
        switch (req.type()) {
            case MULTIPLE_CHOICE -> {
                List<Map<String, Object>> answers = listOfMaps(d.get("answers"));
                answers.removeIf(a -> str(a.get("text")).isBlank());
                if (answers.size() < 2) throw new BadRequestException("Add at least two answers");
                if (answers.stream().noneMatch(a -> Boolean.TRUE.equals(a.get("correct")))) throw new BadRequestException("Mark at least one answer as correct");
                d.put("answers", answers);
            }
            case FILL_GAP -> { if (!GAP.matcher(req.text()).find()) throw new BadRequestException("Use [brackets] to mark the gaps in the question text"); }
            case ORDERING -> {
                List<String> items = listOfStrings(d.get("items"));
                if (items.size() < 2) throw new BadRequestException("Add at least two items in the correct order");
                d.put("items", items);
            }
            case DRAG_DROP -> {
                List<Map<String, Object>> pairs = listOfMaps(d.get("pairs"));
                pairs.removeIf(p -> str(p.get("left")).isBlank() || str(p.get("right")).isBlank());
                if (pairs.size() < 2) throw new BadRequestException("Add at least two pairs");
                d.put("pairs", pairs);
            }
            case FREE_TEXT -> {
                List<Map<String, Object>> options = listOfMaps(d.get("options"));
                options.removeIf(o -> str(o.get("word")).isBlank());
                if (options.isEmpty()) throw new BadRequestException("Add at least one keyword option");
                d.put("options", options);
                d.put("threshold", d.get("threshold") == null ? 1 : ((Number) d.get("threshold")).intValue());
            }
            case RANDOMIZED -> {
                List<String> pool = listOfStrings(d.get("pool"));
                if (pool.isEmpty()) throw new BadRequestException("Select at least one question for the pool");
                for (String id : pool) {
                    Question p = questionEntity(UUID.fromString(id));
                    if (p.getType() == Question.Type.RANDOMIZED) throw new BadRequestException("A pool cannot contain another randomized question");
                }
                d.put("pool", pool);
            }
        }
        q.setType(req.type());
        q.setText(req.text().trim());
        q.setData(d);
        q.setFeedback(req.feedback());
        q.setTags(req.tags());
    }

    /**
     * Импорт в формате AIKEN (как в Moodle / TalentLMS):
     *   What is 2 + 2?
     *   A. 3
     *   B. 4
     *   ANSWER: B
     * dryRun = true — только разобрать и показать (Preview), false — сохранить в банк.
     */
    public List<Map<String, Object>> importAiken(UUID courseId, String data, boolean dryRun, UUID userId, String role) {
        requireManage(course(courseId), userId, role);
        List<Map<String, Object>> parsed = new ArrayList<>();
        String text = null; List<Map<String, Object>> answers = new ArrayList<>();
        for (String raw : (data == null ? "" : data).split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            Matcher opt = Pattern.compile("^([A-Z])[.)]\\s+(.*)$").matcher(line);
            if (line.toUpperCase().startsWith("ANSWER:")) {
                String letter = line.substring(7).trim().toUpperCase();
                if (text == null || answers.isEmpty()) throw new BadRequestException("ANSWER found before any question: " + line);
                int idx = letter.isEmpty() ? -1 : letter.charAt(0) - 'A';
                if (idx < 0 || idx >= answers.size()) throw new BadRequestException("Unknown answer letter in: " + line);
                answers.get(idx).put("correct", true);
                parsed.add(new HashMap<>(Map.of("type", Question.Type.MULTIPLE_CHOICE, "text", text, "data", Map.of("answers", answers))));
                text = null; answers = new ArrayList<>();
            } else if (opt.matches() && text != null) {
                answers.add(new HashMap<>(Map.of("text", opt.group(2).trim(), "correct", false)));
            } else {
                if (text != null && answers.isEmpty()) text += " " + line;   // многострочный вопрос
                else if (text != null) throw new BadRequestException("Question without ANSWER line: " + text);
                else text = line;
            }
        }
        if (text != null) throw new BadRequestException("Question without ANSWER line: " + text);
        if (parsed.isEmpty()) throw new BadRequestException("Nothing to import");
        if (dryRun) return parsed;
        List<Map<String, Object>> created = new ArrayList<>();
        for (Map<String, Object> p : parsed) {
            @SuppressWarnings("unchecked") Map<String, Object> d = (Map<String, Object>) p.get("data");
            created.add(createQuestion(courseId, new QuestionRequest(Question.Type.MULTIPLE_CHOICE, (String) p.get("text"), d, null, "imported"), userId, role));
        }
        return created;
    }

    // =====================================================================
    //  Тест как урок
    // =====================================================================

    /** Add Test: создаём урок типа TEST и строку настроек. Урок неактивен, пока преподаватель не сохранит его (Save and view). */
    public Map<String, Object> createTest(UUID courseId, TestRequest req, UUID userId, String role) {
        Course course = course(courseId);
        requireManage(course, userId, role);
        Unit u = new Unit();
        u.setCourseId(courseId);
        u.setType(Unit.Type.TEST);
        u.setName(req.name().trim());
        u.setActive(req.active() == null || req.active());
        u.setPosition((int) units.countByCourseId(courseId) + 1);
        u.setCreatedBy(userId);
        u = units.save(u);
        Test t = new Test();
        t.setUnitId(u.getId());
        applyTest(t, req);
        tests.save(t);
        replaceQuestions(u.getId(), req.questions());
        publishUnitAdded(course, u, userId);
        return testView(u, userId, role);
    }

    public Map<String, Object> updateTest(UUID unitId, TestRequest req, UUID userId, String role) {
        Unit u = unitService.unit(unitId);
        if (u.getType() != Unit.Type.TEST) throw new BadRequestException("Unit is not a test");
        requireManage(course(u.getCourseId()), userId, role);
        u.setName(req.name().trim());
        if (req.active() != null) u.setActive(req.active());
        units.save(u);
        Test t = tests.findById(unitId).orElseGet(() -> { Test n = new Test(); n.setUnitId(unitId); return n; });
        applyTest(t, req);
        tests.save(t);
        if (req.questions() != null) replaceQuestions(unitId, req.questions());
        return testView(u, userId, role);
    }

    private void applyTest(Test t, TestRequest r) {
        t.setDurationMinutes(r.durationMinutes());
        if (r.passScore() != null) t.setPassScore(r.passScore());
        if (r.shuffleQuestions() != null) t.setShuffleQuestions(r.shuffleQuestions());
        if (r.shuffleAnswers() != null) t.setShuffleAnswers(r.shuffleAnswers());
        if (r.repetitions() != null) t.setRepetitions(r.repetitions());
        t.setMaxAttempts(r.maxAttempts());
        if (r.showCorrectAnswers() != null) t.setShowCorrectAnswers(r.showCorrectAnswers());
        if (r.showGivenAnswers() != null) t.setShowGivenAnswers(r.showGivenAnswers());
        if (r.showLabels() != null) t.setShowLabels(r.showLabels());
        if (r.showScore() != null) t.setShowScore(r.showScore());
        t.setDescription(r.description());
        t.setMessagePassed(r.messagePassed());
        t.setMessageFailed(r.messageFailed());
    }

    private void replaceQuestions(UUID testId, List<TestRequest.Item> items) {
        testQuestions.deleteByTestId(testId);
        testQuestions.flush();
        if (items == null) return;
        int pos = 1;
        Set<UUID> seen = new HashSet<>();
        for (TestRequest.Item it : items) {
            if (it.questionId() == null || !seen.add(it.questionId())) continue;
            questionEntity(it.questionId());
            testQuestions.save(new TestQuestion(testId, it.questionId(), pos++, it.weight() == null || it.weight() < 1 ? 1 : it.weight()));
        }
    }

    /**
     * Экран теста. Преподаватель видит всё (вопросы с ответами, попытки всех учеников);
     * ученик — описание, настройки и свои попытки, но НЕ вопросы (их выдаёт старт попытки).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> testView(UUID unitId, UUID userId, String role) {
        Unit u = unitService.unit(unitId);
        if (u.getType() != Unit.Type.TEST) throw new BadRequestException("Unit is not a test");
        return testView(u, userId, role);
    }

    private Map<String, Object> testView(Unit u, UUID userId, String role) {
        Course course = course(u.getCourseId());
        boolean manage = unitService.canManage(course, userId, role);
        if (!manage && enrollments.findByCourseIdAndUserId(course.getId(), userId).isEmpty()) throw new ForbiddenException("You are not enrolled in this course");
        Test t = tests.findById(u.getId()).orElseGet(() -> { Test n = new Test(); n.setUnitId(u.getId()); return n; });
        List<TestQuestion> tq = testQuestions.findByTestIdOrderByPositionAsc(u.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unitId", u.getId()); m.put("courseId", u.getCourseId()); m.put("courseName", course.getName());
        m.put("name", u.getName()); m.put("active", u.isActive());
        m.put("durationMinutes", t.getDurationMinutes()); m.put("passScore", t.getPassScore());
        m.put("shuffleQuestions", t.isShuffleQuestions()); m.put("shuffleAnswers", t.isShuffleAnswers());
        m.put("repetitions", t.getRepetitions()); m.put("maxAttempts", t.getMaxAttempts());
        m.put("showCorrectAnswers", t.getShowCorrectAnswers()); m.put("showGivenAnswers", t.isShowGivenAnswers());
        m.put("showLabels", t.isShowLabels()); m.put("showScore", t.isShowScore());
        m.put("description", t.getDescription()); m.put("messagePassed", t.getMessagePassed()); m.put("messageFailed", t.getMessageFailed());
        m.put("questionCount", tq.size());
        m.put("totalWeight", tq.stream().mapToInt(TestQuestion::getWeight).sum());
        if (manage) {
            Map<UUID, Question> byId = questions.findAllById(tq.stream().map(TestQuestion::getQuestionId).toList()).stream().collect(Collectors.toMap(Question::getId, x -> x));
            Map<UUID, String> names = new HashMap<>();
            List<Map<String, Object>> qs = new ArrayList<>();
            for (TestQuestion x : tq) {
                Question q = byId.get(x.getQuestionId());
                if (q == null) continue;
                Map<String, Object> qm = questionMap(q, true, names);
                qm.put("position", x.getPosition()); qm.put("weight", x.getWeight());
                qs.add(qm);
            }
            m.put("questions", qs);
            m.put("attempts", attempts.findByTestIdOrderByStartedAtDesc(u.getId()).stream().map(a -> attemptSummary(a, null)).toList());
        } else {
            List<TestAttempt> mine = attempts.findByTestIdAndUserIdOrderByStartedAtDesc(u.getId(), userId);
            m.put("attempts", mine.stream().map(a -> attemptSummary(a, null)).toList());
            TestAttempt open = mine.stream().filter(a -> a.getSubmittedAt() == null).findFirst().orElse(null);
            m.put("openAttemptId", open == null ? null : open.getId());
            m.put("canStart", canStart(t, mine));
            m.put("passed", mine.stream().anyMatch(a -> Boolean.TRUE.equals(a.getPassed())));
            mine.stream().filter(a -> a.getScore() != null).mapToInt(TestAttempt::getScore).max().ifPresent(best -> m.put("bestScore", best));
        }
        return m;
    }

    /** "Allow repetitions": always / if not passed / never + максимум попыток. */
    private boolean canStart(Test t, List<TestAttempt> mine) {
        long finished = mine.stream().filter(a -> a.getSubmittedAt() != null).count();
        if (t.getMaxAttempts() != null && finished >= t.getMaxAttempts()) return false;
        if (finished == 0) return true;
        return switch (t.getRepetitions()) {
            case ALWAYS -> true;
            case IF_NOT_PASSED -> mine.stream().noneMatch(a -> Boolean.TRUE.equals(a.getPassed()));
            case NEVER -> false;
        };
    }

    // =====================================================================
    //  Прохождение (Learner)
    // =====================================================================

    /** Старт попытки: фиксируем время и выдаём вопросы без правильных ответов (перемешанные, если включено). */
    public Map<String, Object> startAttempt(UUID unitId, UUID userId, String role) {
        Unit u = unitService.unit(unitId);
        if (u.getType() != Unit.Type.TEST) throw new BadRequestException("Unit is not a test");
        unitService.start(unitId, userId, role);   // создаёт unit_progress и проверяет доступ / Rules & path
        Test t = tests.findById(unitId).orElseThrow(() -> new NotFoundException("Test not configured"));
        List<TestAttempt> mine = attempts.findByTestIdAndUserIdOrderByStartedAtDesc(unitId, userId);
        TestAttempt open = mine.stream().filter(a -> a.getSubmittedAt() == null).findFirst().orElse(null);
        if (open != null) return attemptView(open, t);            // незавершённая попытка — продолжаем её
        if (!canStart(t, mine)) throw new ForbiddenException("No more attempts allowed for this test");

        List<TestQuestion> tq = testQuestions.findByTestIdOrderByPositionAsc(unitId);
        if (tq.isEmpty()) throw new BadRequestException("The test has no questions yet");
        List<TestQuestion> order = new ArrayList<>(tq);
        if (t.isShuffleQuestions()) Collections.shuffle(order);

        List<Map<String, Object>> presented = new ArrayList<>();
        for (TestQuestion x : order) {
            Question q = questionEntity(x.getQuestionId());
            String sourceId = null;
            if (q.getType() == Question.Type.RANDOMIZED) {                // случайный вопрос из пула
                List<String> pool = listOfStrings(q.getData().get("pool"));
                sourceId = q.getId().toString();
                q = questionEntity(UUID.fromString(pool.get(new Random().nextInt(pool.size()))));
            }
            Map<String, Object> p = present(q, t.isShuffleAnswers());
            p.put("weight", x.getWeight());
            if (sourceId != null) p.put("sourceId", sourceId);
            presented.add(p);
        }
        TestAttempt a = new TestAttempt();
        a.setTestId(unitId);
        a.setUserId(userId);
        a.setQuestions(presented);
        return attemptView(attempts.save(a), t);
    }

    /** Вопрос в "публичном" виде: варианты со стабильными id, без флага correct. */
    private Map<String, Object> present(Question q, boolean shuffleAnswers) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("questionId", q.getId().toString());
        p.put("type", q.getType().name());
        p.put("text", q.getText());
        switch (q.getType()) {
            case MULTIPLE_CHOICE -> {
                List<Map<String, Object>> opts = new ArrayList<>();
                List<Map<String, Object>> answers = listOfMaps(q.getData().get("answers"));
                for (int i = 0; i < answers.size(); i++) opts.add(new LinkedHashMap<>(Map.of("id", i, "text", str(answers.get(i).get("text")))));
                if (shuffleAnswers) Collections.shuffle(opts);
                p.put("options", opts);
                p.put("multiple", answers.stream().filter(a -> Boolean.TRUE.equals(a.get("correct"))).count() > 1);
            }
            case FILL_GAP -> {
                Matcher m = GAP.matcher(q.getText());
                StringBuilder template = new StringBuilder();
                List<Map<String, Object>> gaps = new ArrayList<>();
                int i = 0;
                while (m.find()) {
                    List<String> alts = Arrays.stream(m.group(1).split("\\|")).map(String::trim).toList();
                    List<String> choices = new ArrayList<>(alts);
                    if (shuffleAnswers || true) Collections.shuffle(choices);     // варианты всегда перемешиваем, иначе первый = ответ
                    Map<String, Object> g = new LinkedHashMap<>();
                    g.put("id", i);
                    g.put("choices", alts.size() > 1 ? choices : null);
                    gaps.add(g);
                    m.appendReplacement(template, Matcher.quoteReplacement("{{" + i + "}}"));
                    i++;
                }
                m.appendTail(template);
                p.put("text", template.toString());
                p.put("gaps", gaps);
            }
            case ORDERING -> {
                List<String> items = listOfStrings(q.getData().get("items"));
                List<Map<String, Object>> opts = new ArrayList<>();
                for (int i = 0; i < items.size(); i++) opts.add(new LinkedHashMap<>(Map.of("id", i, "text", items.get(i))));
                Collections.shuffle(opts);                                        // порядок — и есть вопрос
                p.put("options", opts);
            }
            case DRAG_DROP -> {
                List<Map<String, Object>> pairs = listOfMaps(q.getData().get("pairs"));
                List<Map<String, Object>> lefts = new ArrayList<>(), rights = new ArrayList<>();
                for (int i = 0; i < pairs.size(); i++) {
                    lefts.add(new LinkedHashMap<>(Map.of("id", i, "text", str(pairs.get(i).get("left")))));
                    rights.add(new LinkedHashMap<>(Map.of("id", i, "text", str(pairs.get(i).get("right")))));
                }
                Collections.shuffle(rights);
                p.put("lefts", lefts); p.put("rights", rights);
            }
            case FREE_TEXT, RANDOMIZED -> { }
        }
        return p;
    }

    /** Отправка ответов: сравниваем с банком, считаем балл, при прохождении закрываем урок. */
    public Map<String, Object> submit(UUID unitId, UUID attemptId, Map<String, Object> answers, UUID userId, String role) {
        TestAttempt a = attempts.findById(attemptId).orElseThrow(() -> new NotFoundException("Attempt not found"));
        if (!a.getTestId().equals(unitId) || !a.getUserId().equals(userId)) throw new ForbiddenException("Not your attempt");
        if (a.getSubmittedAt() != null) throw new ConflictException("Attempt already submitted");
        Test t = tests.findById(unitId).orElseThrow(() -> new NotFoundException("Test not configured"));
        Map<String, Object> given = answers == null ? Map.of() : answers;

        int total = 0, earned = 0;
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> p : a.getQuestions()) {
            Question q = questionEntity(UUID.fromString((String) p.get("questionId")));
            int weight = ((Number) p.getOrDefault("weight", 1)).intValue();
            Object ans = given.get(q.getId().toString());
            boolean correct = grade(q, ans);
            total += weight;
            if (correct) earned += weight;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("questionId", q.getId().toString()); r.put("correct", correct); r.put("weight", weight);
            r.put("given", ans);
            r.put("correctAnswer", correctAnswer(q));
            r.put("feedback", q.getFeedback());
            results.add(r);
        }
        int score = total == 0 ? 0 : (int) Math.round(earned * 100.0 / total);
        boolean passed = score >= t.getPassScore();
        a.setAnswers(new HashMap<>(given));
        a.setResults(results);
        a.setScore(score);
        a.setPassed(passed);
        a.setSubmittedAt(Instant.now());
        attempts.save(a);

        Unit u = unitService.unit(unitId);
        Course course = course(u.getCourseId());
        if (passed) progress.findByUnitIdAndUserId(unitId, userId).ifPresent(pr -> unitService.markCompleted(u, course, pr, userId));

        UserResponse who = users.getUser(userId).orElse(null);
        Map<String, String> payload = new HashMap<>(Map.of("courseId", course.getId().toString(), "courseName", course.getName(),
                "unitId", unitId.toString(), "unitName", u.getName(), "userId", userId.toString(),
                "score", String.valueOf(score), "passed", String.valueOf(passed)));
        if (who != null) { payload.put("firstName", who.getFirstName()); payload.put("lastName", who.getLastName()); payload.put("email", who.getEmail()); }
        events.publish(Topics.ENROLLMENT_EVENTS, EventType.TEST_COMPLETED, userId.toString(), payload);
        return attemptView(a, t);
    }

    /** Проверка одного ответа. Все типы — "всё или ничего", кроме FREE_TEXT, где баллы за ключевые слова суммируются. */
    @SuppressWarnings("unchecked")
    private boolean grade(Question q, Object ans) {
        if (ans == null) return false;
        Map<String, Object> d = q.getData();
        switch (q.getType()) {
            case MULTIPLE_CHOICE -> {
                List<Map<String, Object>> answers = listOfMaps(d.get("answers"));
                Set<Integer> correct = new HashSet<>();
                for (int i = 0; i < answers.size(); i++) if (Boolean.TRUE.equals(answers.get(i).get("correct"))) correct.add(i);
                Set<Integer> chosen = new HashSet<>();
                if (ans instanceof Collection<?> c) c.forEach(x -> chosen.add(((Number) x).intValue()));
                else if (ans instanceof Number n) chosen.add(n.intValue());
                return chosen.equals(correct);
            }
            case FILL_GAP -> {
                List<String> expected = new ArrayList<>();
                Matcher m = GAP.matcher(q.getText());
                while (m.find()) expected.add(m.group(1).split("\\|")[0].trim());
                if (!(ans instanceof List<?> l) || l.size() != expected.size()) return false;
                for (int i = 0; i < expected.size(); i++) if (!expected.get(i).equalsIgnoreCase(str(l.get(i)).trim())) return false;
                return true;
            }
            case ORDERING -> {
                List<String> items = listOfStrings(d.get("items"));
                if (!(ans instanceof List<?> l) || l.size() != items.size()) return false;
                for (int i = 0; i < l.size(); i++) if (((Number) l.get(i)).intValue() != i) return false;
                return true;
            }
            case DRAG_DROP -> {
                List<Map<String, Object>> pairs = listOfMaps(d.get("pairs"));
                if (!(ans instanceof Map<?, ?> m)) return false;
                for (int i = 0; i < pairs.size(); i++) {
                    Object v = m.get(String.valueOf(i));
                    if (v == null || ((Number) v).intValue() != i) return false;
                }
                return true;
            }
            case FREE_TEXT -> {
                String text = str(ans).toLowerCase();
                int points = 0;
                for (Map<String, Object> o : listOfMaps(d.get("options"))) {
                    boolean hit = Arrays.stream(str(o.get("word")).toLowerCase().split("\\|")).map(String::trim)
                            .anyMatch(w -> "equals".equals(o.get("mode")) ? text.trim().equals(w) : text.contains(w));
                    boolean ok = "not_contains".equals(o.get("mode")) ? !hit : hit;
                    if (ok) points += ((Number) o.getOrDefault("points", 1)).intValue();
                }
                return points >= ((Number) d.getOrDefault("threshold", 1)).intValue();
            }
            default -> { return false; }
        }
    }

    /** Правильный ответ для экрана результатов (показывается по настройке Show correct answers). */
    private Object correctAnswer(Question q) {
        Map<String, Object> d = q.getData();
        return switch (q.getType()) {
            case MULTIPLE_CHOICE -> listOfMaps(d.get("answers")).stream().filter(a -> Boolean.TRUE.equals(a.get("correct"))).map(a -> str(a.get("text"))).toList();
            case FILL_GAP -> { List<String> e = new ArrayList<>(); Matcher m = GAP.matcher(q.getText()); while (m.find()) e.add(m.group(1).split("\\|")[0].trim()); yield e; }
            case ORDERING -> listOfStrings(d.get("items"));
            case DRAG_DROP -> listOfMaps(d.get("pairs")).stream().map(p -> str(p.get("left")) + " → " + str(p.get("right"))).toList();
            case FREE_TEXT -> listOfMaps(d.get("options")).stream().map(o -> o.get("mode") + " \"" + str(o.get("word")) + "\" (+" + o.getOrDefault("points", 1) + ")").toList();
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public Map<String, Object> attempt(UUID unitId, UUID attemptId, UUID userId, String role) {
        TestAttempt a = attempts.findById(attemptId).orElseThrow(() -> new NotFoundException("Attempt not found"));
        Unit u = unitService.unit(unitId);
        boolean manage = unitService.canManage(course(u.getCourseId()), userId, role);
        if (!manage && !a.getUserId().equals(userId)) throw new ForbiddenException("Not your attempt");
        Test t = tests.findById(unitId).orElseThrow(() -> new NotFoundException("Test not configured"));
        Map<String, Object> v = attemptView(a, t);
        if (manage) {   // преподавателю показываем всё
            v.put("results", a.getResults());
            v.put("showCorrect", true); v.put("showGiven", true); v.put("showLabels", true); v.put("showScore", true);
        }
        return v;
    }

    /** Ответ ученику: до отправки — вопросы; после — результат с учётом настроек Completion (что показывать). */
    private Map<String, Object> attemptView(TestAttempt a, Test t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId()); m.put("testId", a.getTestId()); m.put("userId", a.getUserId());
        m.put("startedAt", a.getStartedAt()); m.put("submittedAt", a.getSubmittedAt());
        m.put("durationMinutes", t.getDurationMinutes());
        if (t.getDurationMinutes() != null) m.put("deadline", a.getStartedAt().plus(Duration.ofMinutes(t.getDurationMinutes())));
        m.put("questions", a.getQuestions());
        if (a.getSubmittedAt() == null) return m;
        boolean passed = Boolean.TRUE.equals(a.getPassed());
        m.put("passed", passed);
        m.put("passScore", t.getPassScore());
        m.put("showScore", t.isShowScore());
        if (t.isShowScore()) m.put("score", a.getScore());
        m.put("message", passed ? t.getMessagePassed() : t.getMessageFailed());
        boolean showCorrect = switch (t.getShowCorrectAnswers()) { case ALWAYS -> true; case WHEN_PASSED -> passed; case NEVER -> false; };
        m.put("showCorrect", showCorrect); m.put("showGiven", t.isShowGivenAnswers()); m.put("showLabels", t.isShowLabels());
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> r : a.getResults() == null ? List.<Map<String, Object>>of() : a.getResults()) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("questionId", r.get("questionId"));
            if (t.isShowLabels()) x.put("correct", r.get("correct"));
            if (t.isShowGivenAnswers()) x.put("given", r.get("given"));
            if (showCorrect) { x.put("correctAnswer", r.get("correctAnswer")); x.put("feedback", r.get("feedback")); }
            results.add(x);
        }
        m.put("results", results);
        return m;
    }

    private Map<String, Object> attemptSummary(TestAttempt a, String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId()); m.put("userId", a.getUserId()); m.put("startedAt", a.getStartedAt()); m.put("submittedAt", a.getSubmittedAt());
        m.put("score", a.getScore()); m.put("passed", a.getPassed()); m.put("questions", a.getQuestions().size());
        return m;
    }

    /** Попытки конкретного ученика (для преподавателя) или свои. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> attemptsOf(UUID unitId, UUID targetUserId, UUID userId, String role) {
        Unit u = unitService.unit(unitId);
        boolean manage = unitService.canManage(course(u.getCourseId()), userId, role);
        UUID who = targetUserId == null || !manage ? userId : targetUserId;
        return attempts.findByTestIdAndUserIdOrderByStartedAtDesc(unitId, who).stream().map(a -> attemptSummary(a, null)).toList();
    }

    // ---------- helpers ----------

    private void publishUnitAdded(Course course, Unit u, UUID userId) {
        UserResponse author = users.getUser(userId).orElse(null);
        Map<String, String> payload = new HashMap<>(Map.of("courseId", course.getId().toString(), "courseName", course.getName(),
                "unitId", u.getId().toString(), "unitName", u.getName(), "unitType", "TEST", "userId", userId.toString()));
        if (author != null) { payload.put("firstName", author.getFirstName()); payload.put("lastName", author.getLastName()); payload.put("email", author.getEmail()); }
        events.publish(Topics.COURSE_EVENTS, EventType.UNIT_ADDED, course.getId().toString(), payload);
    }

    private Map<String, Object> questionMap(Question q, boolean withAnswers, Map<UUID, String> courseNames) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId()); m.put("courseId", q.getCourseId());
        m.put("courseName", courseNames.computeIfAbsent(q.getCourseId(), id -> courses.findById(id).map(Course::getName).orElse("?")));
        m.put("type", q.getType()); m.put("text", q.getText());
        if (withAnswers) m.put("data", q.getData());
        m.put("feedback", q.getFeedback()); m.put("tags", q.getTags());
        m.put("createdAt", q.getCreatedAt()); m.put("usedInTests", testQuestions.countByQuestionId(q.getId()));
        return m;
    }

    private Question questionEntity(UUID id) { return questions.findById(id).orElseThrow(() -> new NotFoundException("Question not found: " + id)); }
    private Course course(UUID id) {
        return courses.findById(id).filter(c -> c.getDeletedAt() == null).orElseThrow(() -> new NotFoundException("Course not found: " + id));
    }
    private void requireManage(Course course, UUID userId, String role) {
        if (!unitService.canManage(course, userId, role)) throw new ForbiddenException("You are not an instructor of this course");
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> listOfMaps(Object o) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) if (x instanceof Map<?, ?> m) out.add(new LinkedHashMap<>((Map<String, Object>) m));
        return out;
    }
    private static List<String> listOfStrings(Object o) {
        List<String> out = new ArrayList<>();
        if (o instanceof List<?> l) for (Object x : l) if (x != null && !str(x).isBlank()) out.add(str(x).trim());
        return out;
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
}
