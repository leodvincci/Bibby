package com.penrose.bibby.identity.infrastructure.repository;

import com.penrose.bibby.identity.infrastructure.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRegistrationJpaRepository extends JpaRepository<AppUserEntity, Long> {
  AppUserEntity findByEmail(String username);
}
