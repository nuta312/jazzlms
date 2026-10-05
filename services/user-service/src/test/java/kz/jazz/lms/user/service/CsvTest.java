package kz.jazz.lms.user.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTest {
    @Test
    void parsesQuotedFieldsWithCommasAndEscapedQuotes() {
        assertEquals(List.of("Anna", "O\"Neil, Jr", "a@b.kz", "anna"),
                Csv.parseLine("Anna,\"O\"\"Neil, Jr\",a@b.kz,anna"));
    }

    @Test
    void acceptsSemicolonSeparator() {   // Excel в русской локали сохраняет CSV через ';'
        assertEquals(List.of("A", "B", "c@d.kz", "ab"), Csv.parseLine("A;B;c@d.kz;ab"));
    }

    @Test
    void escapeDoublesQuotes() {
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""));
    }
}
