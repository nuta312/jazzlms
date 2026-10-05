package kz.jazz.lms.user.web;

import jakarta.validation.Valid;
import kz.jazz.lms.user.dto.SettingsDto;
import kz.jazz.lms.user.service.SettingsService;
import org.springframework.web.bind.annotation.*;

/** Account & Settings → Basic settings / Users. Путь /api/settings закрыт в gateway для всех, кроме админов. */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settings;

    public SettingsController(SettingsService settings) { this.settings = settings; }

    @GetMapping
    public SettingsDto get() { return SettingsDto.from(settings.get()); }

    @PutMapping
    public SettingsDto update(@Valid @RequestBody SettingsDto dto) { return SettingsDto.from(settings.update(dto)); }
}
