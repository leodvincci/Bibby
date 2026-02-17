package com.penrose.bibby.web.registration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.penrose.bibby.identity.api.RegisterUserResult;
import com.penrose.bibby.identity.core.application.UserRegistrationService;
import com.penrose.bibby.web.controllers.registration.UserRegistrationController;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
              "email": "ldpenrose@gmail.com",
                "password": "SecureP@"
            }
            """;

    RegisterUserResult expectedResult = new RegisterUserResult(1L, "ldpenrose@gmail.com");
    when(userRegistrationService.registerUser(ArgumentMatchers.any())).thenReturn(expectedResult);
    mockMvc
        .perform(
            post("/api/v1/user/registration/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").isNotEmpty())
        .andExpect(jsonPath("$.email").value("ldpenrose@gmail.com"));
  }
}
