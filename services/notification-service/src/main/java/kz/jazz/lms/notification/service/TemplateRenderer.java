package kz.jazz.lms.notification.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Минимальный шаблонизатор: заменяет {{key}} на значение из payload события. */
public final class TemplateRenderer {
    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private TemplateRenderer() {}

    public static String render(String template, Map<String, String> vars) {
        if (template == null) return "";
        Matcher m = VAR.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = vars.getOrDefault(m.group(1), "");
            m.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
