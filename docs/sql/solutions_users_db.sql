-- Решения задач 1–6 (база users_db). Запускать: выделить базу users_db, Cmd+] / Ctrl+].

-- 1. Активные пользователи по дате регистрации
SELECT username, first_name, last_name, user_type, created_at
FROM users
WHERE active = TRUE
ORDER BY created_at;

-- 2. Сколько пользователей каждой роли
SELECT user_type, COUNT(*) AS users_count
FROM users
GROUP BY user_type
ORDER BY users_count DESC;

-- 3. Кто не заходил больше 7 дней или не заходил никогда
SELECT username, last_login_at
FROM users
WHERE last_login_at IS NULL OR last_login_at < NOW() - INTERVAL '7 days'
ORDER BY last_login_at NULLS FIRST;

-- 4. Состав группы: участники с датой добавления
SELECT g.name AS group_name, u.first_name || ' ' || u.last_name AS member, u.user_type, ug.added_at
FROM groups g
JOIN user_groups ug ON ug.group_id = g.id
JOIN users u ON u.id = ug.user_id
ORDER BY g.name, ug.added_at;

-- 5. Группы и число участников (пустые группы тоже)
SELECT g.name, COUNT(ug.user_id) AS members
FROM groups g
LEFT JOIN user_groups ug ON ug.group_id = g.id
GROUP BY g.id, g.name
ORDER BY members DESC, g.name;

-- 6. Пользователи, не состоящие ни в одной ветке (два способа)
SELECT u.username
FROM users u
LEFT JOIN user_branches ub ON ub.user_id = u.id
WHERE ub.user_id IS NULL;

SELECT u.username
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM user_branches ub WHERE ub.user_id = u.id);
