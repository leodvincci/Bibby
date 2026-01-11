package com.penrose.bibby.library.registration;

import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationRequestDTO;

public class UserRegistrationMapper {


    public static AppUserEntity toEntity(UserRegistrationRequestDTO dto) {
        AppUserEntity entity = new AppUserEntity();
        entity.setPassword(dto.getPassword());
        entity.setEmail(dto.getEmail());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        return entity;
    }

}
