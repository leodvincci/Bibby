package com.penrose.bibby.library.registration.infrastructure.repository;

import com.penrose.bibby.library.registration.infrastructure.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRegistrationJpaRepository extends JpaRepository<AppUserEntity, Long> {
  AppUserEntity findByEmail(String username);
}
