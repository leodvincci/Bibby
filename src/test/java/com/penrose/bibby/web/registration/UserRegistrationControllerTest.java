package com.penrose.bibby.web.registration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserRegistrationController.class)
class UserRegistrationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void registerUser_returns201_andReturnsResponseBody() throws Exception {
        String payload = """
            {
              "email": "ldpenrose@gmail.com",
              "firstName": "Leo",
              "lastName": "Penrose"
            }
            """;

        mockMvc.perform(post("/api/v1/user/registration/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ldpenrose@gmail.com"))
                .andExpect(jsonPath("$.firstName").value("Leo"))
                .andExpect(jsonPath("$.lastName").value("Penrose"));
    }
}