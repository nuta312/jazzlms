package kz.jazz.lms.user.config;

import kz.jazz.lms.user.domain.User;
import kz.jazz.lms.user.domain.UserType;
import kz.jazz.lms.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** При первом запуске создаём admin / admin123 и преподавателя instructor / instructor123. */
@Configuration
public class DataSeeder {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (!repo.existsByUsername("instructor")) {
                User t = new User();
                t.setFirstName("Ivan");
                t.setLastName("Instructor");
                t.setEmail("instructor@jazzlms.local");
                t.setUsername("instructor");
                t.setPasswordHash(encoder.encode("instructor123"));
                t.setUserType(UserType.TRAINER);
                repo.save(t);
                log.info("Seeded demo instructor: instructor / instructor123");
            }
            if (repo.existsByUsername("admin")) return;
            User admin = new User();
            admin.setFirstName("Default");
            admin.setLastName("Admin");
            admin.setEmail("admin@jazzlms.local");
            admin.setUsername("admin");
            admin.setPasswordHash(encoder.encode("admin123"));
            admin.setUserType(UserType.SUPER_ADMIN);
            repo.save(admin);
            log.info("Seeded default admin: admin / admin123");
        };
    }
}
