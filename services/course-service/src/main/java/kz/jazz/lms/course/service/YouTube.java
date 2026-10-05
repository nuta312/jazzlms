package kz.jazz.lms.course.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Достаёт id ролика из разных форматов ссылок YouTube и строит embed-URL для iframe. */
public final class YouTube {
    private static final Pattern ID = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\\.be/)([A-Za-z0-9_-]{11})");

    private YouTube() {}

    public static Optional<String> videoId(String url) {
        if (url == null) return Optional.empty();
        Matcher m = ID.matcher(url.trim());
        return m.find() ? Optional.of(m.group(1)) : Optional.empty();
    }

    public static String embedUrl(String url) {
        return videoId(url).map(id -> "https://www.youtube.com/embed/" + id).orElse(null);
    }
}
