package com.penrose.bibby.library.registration;

import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationRequestDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

  private final UserRegistrationJpaRepository userRegistrationJpaRepository;
  private final BCryptPasswordEncoder Bcrypt = new BCryptPasswordEncoder(14);

  public UserRegistrationService(UserRegistrationJpaRepository userRegistrationJpaRepository) {
    this.userRegistrationJpaRepository = userRegistrationJpaRepository;
  }

  public void registerUser(UserRegistrationRequestDTO userRegistrationRequestDTO) {
    AppUserEntity appUserEntity = UserRegistrationMapper.toEntity(userRegistrationRequestDTO);
    appUserEntity.setPassword(Bcrypt.encode(userRegistrationRequestDTO.getPassword()));
    userRegistrationJpaRepository.save(appUserEntity);
  }
}
