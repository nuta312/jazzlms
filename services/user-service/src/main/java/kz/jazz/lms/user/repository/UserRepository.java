package kz.jazz.lms.user.repository;

import kz.jazz.lms.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA: по имени метода генерируется SQL-запрос.
 * findByUsername -> SELECT * FROM users WHERE username = ?
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    long countByActiveTrue();
    boolean existsByEmail(String email);
    /**
     * Поиск + фильтр по статусу одним запросом. Когда условий несколько и часть необязательна,
     * имя метода становится нечитаемым — тогда пишут JPQL в @Query.
     * pattern = "%текст%" (или "%" — без поиска); filterActive=false отключает фильтр по статусу.
     */
    @Query("""
            select u from User u
            where (:filterActive = false or u.active = :active)
              and (lower(u.firstName) like :pattern or lower(u.lastName) like :pattern
                   or lower(u.email) like :pattern or lower(u.username) like :pattern)
            order by u.lastName, u.firstName""")
    List<User> search(@Param("pattern") String pattern, @Param("filterActive") boolean filterActive, @Param("active") boolean active);
}
