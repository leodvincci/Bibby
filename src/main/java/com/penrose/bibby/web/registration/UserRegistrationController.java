package com.penrose.bibby.web.registration;

import com.penrose.bibby.library.registration.UserRegistrationRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/registration")
public class UserRegistrationController {

    @PostMapping("/register")
    public void registerUser(@RequestBody UserRegistrationRequest userRegistrationRequest) {
        System.out.println("Registering user: " + userRegistrationRequest.getUsername());
    }
}
