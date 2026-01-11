package com.penrose.bibby.web.registration;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/registration")
public class UserRegistrationController {

    public void registerUser() {
        System.out.println("User registration endpoint hit");
    }

}
