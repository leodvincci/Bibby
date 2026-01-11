package com.penrose.bibby.web.registration;
import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationRequestDTO;
import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/registration")
public class UserRegistrationController {

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDTO> registerUser(@RequestBody UserRegistrationRequestDTO userRegistrationRequestDTO) {
        UserRegistrationResponseDTO userRegistrationResponseDTO =
                new UserRegistrationResponseDTO(
                        userRegistrationRequestDTO.getEmail(),
                        userRegistrationRequestDTO.getFirstName(),
                        userRegistrationRequestDTO.getLastName()
                );

        System.out.println("Registering user: " + userRegistrationResponseDTO.email());

        return ResponseEntity.status(HttpStatus.CREATED).body(userRegistrationResponseDTO);
    }
}
