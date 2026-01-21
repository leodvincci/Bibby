package com.penrose.bibby.library.registration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

  private final UserRegistrationJpaRepository userRegistrationJpaRepository;
  private final BCryptPasswordEncoder Bcrypt = new BCryptPasswordEncoder(14);

  public UserRegistrationService(UserRegistrationJpaRepository userRegistrationJpaRepository) {
    this.userRegistrationJpaRepository = userRegistrationJpaRepository;
  }

  public void registerUser(UserRegistrationRequestCommand userRegistrationRequestCommand) {
    AppUserEntity appUserEntity = UserRegistrationMapper.toEntity(userRegistrationRequestCommand);
    appUserEntity.setPassword(Bcrypt.encode(userRegistrationRequestCommand.password()));
    userRegistrationJpaRepository.save(appUserEntity);
  }
}
