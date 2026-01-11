package com.penrose.bibby.web.registration;

import com.penrose.bibby.library.registration.UserRegistrationService;
import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationRequestDTO;
import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationResponseDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/registration")
public class UserRegistrationController {

  Logger logger = org.slf4j.LoggerFactory.getLogger(UserRegistrationController.class);
  UserRegistrationService userRegistrationService;

  public UserRegistrationController(UserRegistrationService userRegistrationService) {
    this.userRegistrationService = userRegistrationService;
  }

  @PostMapping("/register")
  public ResponseEntity<UserRegistrationResponseDTO> registerUser(
      @Valid @RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {

    userRegistrationService.registerUser(userRegistrationRequestDTO);

    UserRegistrationResponseDTO userRegistrationResponseDTO =
        new UserRegistrationResponseDTO(
            userRegistrationRequestDTO.getEmail(),
            userRegistrationRequestDTO.getFirstName(),
            userRegistrationRequestDTO.getLastName());

    logger.info("Registering user: {}", userRegistrationResponseDTO.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(userRegistrationResponseDTO);
  }
}
