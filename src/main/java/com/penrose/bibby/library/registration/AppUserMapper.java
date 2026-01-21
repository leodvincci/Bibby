package com.penrose.bibby.library.registration;

import com.penrose.bibby.library.registration.contracts.dtos.RegisterUserRequestDTO;
import com.penrose.bibby.library.registration.contracts.dtos.RegisterUserResponseDTO;
import jakarta.validation.Valid;

public class AppUserMapper {

  public static AppUserEntity toEntity(RegisterUserRequestDTO dto) {
    AppUserEntity entity = new AppUserEntity();
    entity.setPassword(dto.getPassword());
    entity.setEmail(dto.getEmail());

    return entity;
  }

  public static RegisterUserResponseDTO toDTO(RegisterUserCommand command) {
    return  new RegisterUserResponseDTO(command.email());
  }

  public static AppUserEntity toEntity(RegisterUserCommand command) {
    AppUserEntity entity = new AppUserEntity();
    entity.setPassword(command.password());
    entity.setEmail(command.email());
    return entity;
  }

  public static RegisterUserCommand toCommand(
      @Valid RegisterUserRequestDTO registerUserRequestDTO) {
    return new RegisterUserCommand(
        registerUserRequestDTO.getEmail(), registerUserRequestDTO.getPassword());
  }
}
