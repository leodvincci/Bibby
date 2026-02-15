package com.penrose.bibby.web.controllers.registration;

import com.penrose.bibby.library.registration.api.RegisterUserCommand;
import com.penrose.bibby.library.registration.api.RegisterUserResult;
import com.penrose.bibby.library.registration.api.dtos.RegisterUserRequestDTO;
import com.penrose.bibby.library.registration.api.dtos.RegisterUserResponseDTO;
import com.penrose.bibby.library.registration.core.application.UserRegistrationService;
import com.penrose.bibby.library.registration.infrastructure.mapping.AppUserMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/registration")
public class UserRegistrationController {

  Logger logger = org.slf4j.LoggerFactory.getLogger(UserRegistrationController.class);
  UserRegistrationService userRegistrationService;

  public UserRegistrationController(UserRegistrationService userRegistrationService) {
    this.userRegistrationService = userRegistrationService;
  }

  @CrossOrigin(origins = "*")
  @PostMapping("/register")
  public ResponseEntity<RegisterUserResponseDTO> registerUser(
      @Valid @RequestBody RegisterUserRequestDTO registerUserRequestDTO) {
    RegisterUserCommand registerUserCommand = AppUserMapper.toCommand(registerUserRequestDTO);
    logger.info("Registering user");

    RegisterUserResult registerUserResult =
        userRegistrationService.registerUser(registerUserCommand);

    logger.info("User registered with ID: {}", registerUserResult.userId());
    RegisterUserResponseDTO registerUserResponseDTO =
        AppUserMapper.toResponseDTO(registerUserResult);

    return ResponseEntity.status(HttpStatus.CREATED).body(registerUserResponseDTO);
  }
}
