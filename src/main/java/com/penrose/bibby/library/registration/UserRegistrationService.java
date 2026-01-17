package com.penrose.bibby.library.registration;

import com.penrose.bibby.library.registration.contracts.dtos.UserLoginRequestDTO;
import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationRequestDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {
  private final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(UserRegistrationService.class);

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

  public Object findUserByEmail(String userEmail) {
    return userRegistrationJpaRepository.findByEmail(userEmail);
  }

  public void verifyUser(UserLoginRequestDTO userLoginRequestDTO) {
    var u = findUserByEmail(userLoginRequestDTO.userEmail());
    if (u != null) {
      logger.info("User found: {}", userLoginRequestDTO.userEmail());
    } else {
      logger.info("User not found: {}", userLoginRequestDTO.userEmail());
    }
  }
}
