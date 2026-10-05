package kz.jazz.lms.course.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class YouTubeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=10s",
            "https://youtu.be/dQw4w9WgXcQ?si=abc",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://youtube.com/shorts/dQw4w9WgXcQ"})
    void extractsIdFromAllLinkFormats(String url) {
        assertEquals("dQw4w9WgXcQ", YouTube.videoId(url).orElseThrow());
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", YouTube.embedUrl(url));
    }

    @Test
    void rejectsOtherSites() {
        assertTrue(YouTube.videoId("https://vimeo.com/12345").isEmpty());
        assertTrue(YouTube.videoId(null).isEmpty());
        assertNull(YouTube.embedUrl("not a link"));
    }
}
