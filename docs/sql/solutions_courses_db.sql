-- Решения задач 7–24 (база courses_db).

-- 7. Курсы с категорией и ценой (курсы без категории тоже)
SELECT c.name AS course, COALESCE(cat.name, '(без категории)') AS category, c.price, c.level
FROM courses c
LEFT JOIN categories cat ON cat.id = c.category_id
WHERE c.deleted_at IS NULL
ORDER BY c.name;

-- 8. Число учеников на каждом курсе (курсы без учеников тоже)
SELECT c.name, COUNT(e.id) FILTER (WHERE e.role = 'LEARNER') AS learners
FROM courses c
LEFT JOIN enrollments e ON e.course_id = c.id
WHERE c.deleted_at IS NULL
GROUP BY c.id, c.name
ORDER BY learners DESC, c.name;

-- 9. Средний прогресс и число завершивших — только курсы, где есть хоть один ученик
SELECT c.name,
       COUNT(*)                                             AS learners,
       ROUND(AVG(e.progress), 1)                            AS avg_progress,
       COUNT(*) FILTER (WHERE e.completed_at IS NOT NULL)   AS completed
FROM courses c
JOIN enrollments e ON e.course_id = c.id AND e.role = 'LEARNER'
WHERE c.deleted_at IS NULL
GROUP BY c.id, c.name
HAVING COUNT(*) >= 1
ORDER BY avg_progress DESC;

-- 10. Уроки курса и сколько учеников прошли каждый
SELECT c.name AS course, u.position, u.name AS unit, u.type,
       COUNT(p.id) FILTER (WHERE p.completed_at IS NOT NULL) AS completed_by
FROM courses c
JOIN units u ON u.course_id = c.id
LEFT JOIN unit_progress p ON p.unit_id = u.id
WHERE c.deleted_at IS NULL
GROUP BY c.id, c.name, u.id, u.position, u.name, u.type
ORDER BY c.name, u.position;

-- 11. Время обучения по курсам (сумма completed_at - started_at), в минутах
SELECT c.name,
       ROUND(SUM(EXTRACT(EPOCH FROM (p.completed_at - p.started_at))) / 60, 1) AS minutes
FROM unit_progress p
JOIN units u ON u.id = p.unit_id
JOIN courses c ON c.id = u.course_id
WHERE p.completed_at IS NOT NULL
GROUP BY c.id, c.name
ORDER BY minutes DESC;

-- 12. «Сложные» уроки: доля начавших, но не завершивших
SELECT c.name AS course, u.name AS unit,
       COUNT(p.id)                                           AS started,
       COUNT(p.id) FILTER (WHERE p.completed_at IS NULL)     AS not_finished,
       ROUND(100.0 * COUNT(p.id) FILTER (WHERE p.completed_at IS NULL) / NULLIF(COUNT(p.id), 0), 0) AS drop_pct
FROM units u
JOIN courses c ON c.id = u.course_id
LEFT JOIN unit_progress p ON p.unit_id = u.id
GROUP BY c.name, u.id, u.name
HAVING COUNT(p.id) > 0
ORDER BY drop_pct DESC NULLS LAST;

-- 13. Статистика по тестам: попытки, средний балл, доля сданных
SELECT c.name AS course, u.name AS test, t.pass_score,
       COUNT(a.id) FILTER (WHERE a.submitted_at IS NOT NULL)        AS attempts,
       ROUND(AVG(a.score), 1)                                       AS avg_score,
       ROUND(100.0 * COUNT(*) FILTER (WHERE a.passed) / NULLIF(COUNT(a.id) FILTER (WHERE a.submitted_at IS NOT NULL), 0), 0) AS pass_pct
FROM tests t
JOIN units u ON u.id = t.unit_id
JOIN courses c ON c.id = u.course_id
LEFT JOIN test_attempts a ON a.test_id = t.unit_id
GROUP BY c.name, u.name, t.pass_score
ORDER BY attempts DESC;

-- 14. Лучший результат каждого ученика по каждому тесту
SELECT u.name AS test, a.user_id, MAX(a.score) AS best_score, COUNT(*) AS tries,
       BOOL_OR(a.passed) AS passed
FROM test_attempts a
JOIN units u ON u.id = a.test_id
WHERE a.submitted_at IS NOT NULL
GROUP BY u.name, a.user_id
ORDER BY u.name, best_score DESC;

-- 15. Состав теста: вопросы по порядку с типом и весом
SELECT u.name AS test, tq.position, q.type, LEFT(q.text, 60) AS question, tq.weight
FROM test_questions tq
JOIN units u ON u.id = tq.test_id
JOIN questions q ON q.id = tq.question_id
ORDER BY u.name, tq.position;

-- 16. Сертификаты: курс, шаблон, дата
SELECT cert.code, c.name AS course, t.name AS template, cert.user_id, cert.issued_at
FROM certificates cert
JOIN courses c ON c.id = cert.course_id
LEFT JOIN certificate_templates t ON t.id = cert.template_id
ORDER BY cert.issued_at DESC;

-- 17. Завершили курс с шаблоном сертификата, но сертификата нет
SELECT c.name, e.user_id, e.completed_at
FROM enrollments e
JOIN courses c ON c.id = e.course_id
LEFT JOIN certificates cert ON cert.course_id = e.course_id AND cert.user_id = e.user_id
WHERE e.role = 'LEARNER' AND e.completed_at IS NOT NULL
  AND c.certificate_template_id IS NOT NULL
  AND cert.id IS NULL;

-- 18. Рейтинг учеников внутри курса по прогрессу (оконная функция)
SELECT c.name AS course, e.user_id, e.progress,
       RANK() OVER (PARTITION BY e.course_id ORDER BY e.progress DESC) AS place
FROM enrollments e
JOIN courses c ON c.id = e.course_id
WHERE e.role = 'LEARNER'
ORDER BY c.name, place;

-- 19. Последний записавшийся ученик каждого курса (CTE + ROW_NUMBER)
WITH ranked AS (
    SELECT e.*, ROW_NUMBER() OVER (PARTITION BY e.course_id ORDER BY e.enrolled_at DESC) AS rn
    FROM enrollments e
    WHERE e.role = 'LEARNER'
)
SELECT c.name, r.user_id, r.enrolled_at
FROM ranked r
JOIN courses c ON c.id = r.course_id
WHERE r.rn = 1
ORDER BY r.enrolled_at DESC;

-- 20. JSONB: число вариантов и число правильных в вопросах Multiple choice
SELECT LEFT(q.text, 50) AS question,
       jsonb_array_length(q.data -> 'answers') AS options,
       (SELECT COUNT(*) FROM jsonb_array_elements(q.data -> 'answers') a WHERE (a ->> 'correct')::boolean) AS correct_options
FROM questions q
WHERE q.type = 'MULTIPLE_CHOICE'
ORDER BY correct_options DESC;

-- 21. JSONB: самые сложные вопросы — сколько раз на них ответили неверно
SELECT LEFT(q.text, 50) AS question, q.type,
       COUNT(*) FILTER (WHERE NOT (r ->> 'correct')::boolean) AS wrong,
       COUNT(*)                                                AS answered,
       ROUND(100.0 * COUNT(*) FILTER (WHERE NOT (r ->> 'correct')::boolean) / COUNT(*), 0) AS wrong_pct
FROM test_attempts a
CROSS JOIN LATERAL jsonb_array_elements(a.results) AS r
JOIN questions q ON q.id = (r ->> 'questionId')::uuid
WHERE a.submitted_at IS NOT NULL
GROUP BY q.id, q.text, q.type
ORDER BY wrong_pct DESC, answered DESC;

-- 22. Дерево категорий: категория и её родитель (self join)
SELECT child.name AS category, parent.name AS parent, child.price
FROM categories child
LEFT JOIN categories parent ON parent.id = child.parent_id
ORDER BY parent.name NULLS FIRST, child.name;

-- 23. Имена учеников из другой базы: прямой JOIN невозможен, можно через dblink
CREATE EXTENSION IF NOT EXISTS dblink;
SELECT c.name AS course, u.username, e.progress
FROM enrollments e
JOIN courses c ON c.id = e.course_id
JOIN dblink('dbname=users_db user=lms password=lms host=localhost',
            'SELECT id, username FROM users') AS u(id uuid, username text) ON u.id = e.user_id
WHERE e.role = 'LEARNER'
ORDER BY c.name, u.username;

-- 24. Изменение данных в транзакции с откатом
BEGIN;
INSERT INTO categories (id, name, price) VALUES (gen_random_uuid(), 'SQL practice', 0);
UPDATE courses SET price = COALESCE(price, 0) + 10 WHERE deleted_at IS NULL;
SELECT name, price FROM courses WHERE deleted_at IS NULL;
ROLLBACK;
SELECT COUNT(*) AS categories_after_rollback FROM categories;

-- Бонус: план запроса
EXPLAIN ANALYZE
SELECT c.name, COUNT(e.id) FROM courses c LEFT JOIN enrollments e ON e.course_id = c.id GROUP BY c.id, c.name;
