package com.penrose.bibby.library.registration;

import com.penrose.bibby.library.registration.contracts.dtos.RegisterUserRequestDTO;
import jakarta.validation.Valid;

public class UserRegistrationMapper {

  public static AppUserEntity toEntity(RegisterUserRequestDTO dto) {
    AppUserEntity entity = new AppUserEntity();
    entity.setPassword(dto.getPassword());
    entity.setEmail(dto.getEmail());

    return entity;
  }

  public static AppUserEntity toEntity(UserRegistrationRequestCommand command) {
    AppUserEntity entity = new AppUserEntity();
    entity.setPassword(command.password());
    entity.setEmail(command.email());
    return entity;
  }

  public static UserRegistrationRequestCommand toCommand(
      @Valid RegisterUserRequestDTO registerUserRequestDTO) {
    return new UserRegistrationRequestCommand(
        registerUserRequestDTO.getEmail(), registerUserRequestDTO.getPassword());
  }
}
