package kz.jazz.lms.user.service;

import java.util.ArrayList;
import java.util.List;

/** Минимальный CSV (RFC 4180): поля в кавычках, кавычка внутри поля удваивается. */
public final class Csv {
    private Csv() {}

    public static String escape(Object value) {
        String s = value == null ? "" : value.toString();
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cur.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',' || c == ';') { out.add(cur.toString().trim()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString().trim());
        return out;
    }
}
