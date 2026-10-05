package kz.jazz.lms.notification.domain;

/** Кому отправлять (как в TalentLMS: Related user / Account owner / Course instructors). */
public enum Recipient {
    RELATED_USER,       // пользователь из события (payload.email)
    ACCOUNT_OWNER,      // владелец портала (lms.account-owner-email)
    COURSE_INSTRUCTORS  // преподаватели курса (в учебном проекте — заглушка, попадает в лог)
}
