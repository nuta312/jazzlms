package kz.jazz.lms.notification.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateRendererTest {
    @Test
    void replacesPlaceholders() {
        String out = TemplateRenderer.render("Hi {{firstName}}, welcome to {{ portalName }}!",
                Map.of("firstName", "Aida", "portalName", "JazzLMS"));
        assertEquals("Hi Aida, welcome to JazzLMS!", out);
    }

    @Test
    void unknownPlaceholderBecomesEmpty() {
        assertEquals("Hi !", TemplateRenderer.render("Hi {{nope}}!", Map.of()));
    }
}
