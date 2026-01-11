package com.penrose.bibby.web.registration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.penrose.bibby.library.registration.UserRegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    value = UserRegistrationController.class,
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = EnableWebSecurity.class))
@AutoConfigureMockMvc(addFilters = false)
class UserRegistrationControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean UserRegistrationService userRegistrationService;

  @Test
  void registerUser_returns201_andReturnsResponseBody() throws Exception {
    String payload =
        """
            {
              "username": "ldpenrose",
              "email": "ldpenrose@gmail.com",
              "firstName": "Leo",
              "lastName": "Penrose"
            }
            """;

    mockMvc
        .perform(
            post("/api/v1/user/registration/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("ldpenrose@gmail.com"))
        .andExpect(jsonPath("$.firstName").value("Leo"))
        .andExpect(jsonPath("$.lastName").value("Penrose"));
  }
}
