package com.penrose.bibby.library.registration;

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
