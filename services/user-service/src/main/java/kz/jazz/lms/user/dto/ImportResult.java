package kz.jazz.lms.user.dto;

import java.util.List;

/** Итог импорта: сколько создано и какие строки пропущены (с причиной). */
public record ImportResult(int created, List<Skipped> skipped) {
    public record Skipped(int line, String value, String reason) {}
}
