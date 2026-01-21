package com.penrose.bibby.web.registration;

import com.penrose.bibby.library.registration.UserRegistrationMapper;
import com.penrose.bibby.library.registration.UserRegistrationRequestCommand;
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
    UserRegistrationRequestCommand userRegistrationRequestCommand =
        UserRegistrationMapper.toCommand(registerUserRequestDTO);
    userRegistrationService.registerUser(userRegistrationRequestCommand);

    RegisterUserResponseDTO registerUserResponseDTO =
        new RegisterUserResponseDTO(registerUserRequestDTO.getEmail());

    logger.info("Registering user: {}", registerUserResponseDTO.email());
    return ResponseEntity.status(HttpStatus.CREATED).body(registerUserResponseDTO);
  }
}
