package kz.jazz.lms.events;

/**
 * Типы доменных событий. Notification-service сопоставляет их с правилами
 * уведомлений (аналог "Event" в TalentLMS Events Engine).
 */
public enum EventType {
    USER_CREATED,        // админ создал пользователя
    USER_SIGNED_UP,      // пользователь зарегистрировался сам
    USER_LOGGED_IN,
    COURSE_CREATED,
    COURSE_UPDATED,
    USER_ENROLLED,       // пользователя добавили на курс
    COURSE_COMPLETED,    // пользователь завершил курс
    ASSIGNMENT_SUBMITTED,
    UNIT_ADDED,          // преподаватель добавил урок в курс
    UNIT_COMPLETED,      // ученик прошёл урок
    COURSE_DELETED,      // мягкое удаление (можно отменить)
    COURSE_RESTORED,     // Undo delete
    USER_DELETED,
    USER_IMPERSONATED,   // администратор вошёл под другим пользователем (Log into account)
    CERTIFICATE_ISSUED,  // ученик завершил курс с сертификатом — выдан сертификат
    TEST_COMPLETED       // ученик отправил тест (payload: score, passed)
}
