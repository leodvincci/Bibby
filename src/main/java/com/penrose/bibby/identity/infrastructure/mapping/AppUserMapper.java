package com.penrose.bibby.identity.infrastructure.mapping;

import com.penrose.bibby.identity.api.RegisterUserCommand;
import com.penrose.bibby.identity.api.RegisterUserResult;
import com.penrose.bibby.identity.api.dtos.RegisterUserRequestDTO;
import com.penrose.bibby.identity.api.dtos.RegisterUserResponseDTO;
import com.penrose.bibby.identity.infrastructure.entity.AppUserEntity;
import jakarta.validation.Valid;

public class AppUserMapper {

  public static AppUserEntity toEntity(RegisterUserRequestDTO dto) {
    AppUserEntity entity = new AppUserEntity();
    entity.setPassword(dto.getPassword());
    entity.setEmail(dto.getEmail());

    return entity;
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

  public static RegisterUserResponseDTO toResponseDTO(RegisterUserResult registerUserResult) {
    return new RegisterUserResponseDTO(registerUserResult.userId(), registerUserResult.email());
  }
}
