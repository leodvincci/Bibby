package com.penrose.bibby.library.registration;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRegistrationJpaRepository extends JpaRepository<AppUserEntity, Long> {
  AppUserEntity findByEmail(String username);
}
