package com.penrose.bibby.library.registration.core.application;

import com.penrose.bibby.library.registration.api.RegisterUserCommand;
import com.penrose.bibby.library.registration.api.RegisterUserResult;
import com.penrose.bibby.library.registration.infrastructure.entity.AppUserEntity;
import com.penrose.bibby.library.registration.infrastructure.mapping.AppUserMapper;
import com.penrose.bibby.library.registration.infrastructure.repository.UserRegistrationJpaRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

  private final UserRegistrationJpaRepository userRegistrationJpaRepository;
  private final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder(14);

  public UserRegistrationService(UserRegistrationJpaRepository userRegistrationJpaRepository) {
    this.userRegistrationJpaRepository = userRegistrationJpaRepository;
  }

  public RegisterUserResult registerUser(RegisterUserCommand registerUserCommand) {
    AppUserEntity appUserEntity = AppUserMapper.toEntity(registerUserCommand);
    appUserEntity.setPassword(bCryptPasswordEncoder.encode(registerUserCommand.password()));
    appUserEntity = userRegistrationJpaRepository.save(appUserEntity);
    return new RegisterUserResult(appUserEntity.getId(), appUserEntity.getEmail());
  }
}
