package com.penrose.bibby.web.registration;

import com.penrose.bibby.library.registration.AppUserMapper;
import com.penrose.bibby.library.registration.RegisterUserCommand;
import com.penrose.bibby.library.registration.RegisterUserResult;
import com.penrose.bibby.library.registration.UserRegistrationService;
import com.penrose.bibby.library.registration.contracts.dtos.RegisterUserRequestDTO;
import com.penrose.bibby.library.registration.contracts.dtos.RegisterUserResponseDTO;
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
