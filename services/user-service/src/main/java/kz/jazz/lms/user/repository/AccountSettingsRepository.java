package kz.jazz.lms.user.repository;

import kz.jazz.lms.user.domain.AccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSettingsRepository extends JpaRepository<AccountSettings, Short> {}
