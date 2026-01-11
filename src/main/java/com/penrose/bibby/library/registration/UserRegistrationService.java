package com.penrose.bibby.library.registration;

import com.penrose.bibby.library.registration.contracts.dtos.UserRegistrationRequestDTO;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

    private final UserRegistrationJpaRepository userRegistrationJpaRepository;

    public UserRegistrationService(UserRegistrationJpaRepository userRegistrationJpaRepository) {
        this.userRegistrationJpaRepository = userRegistrationJpaRepository;
    }


    public void registerUser(UserRegistrationRequestDTO userRegistrationRequestDTO) {
        AppUserEntity appUserEntity = UserRegistrationMapper.toEntity(userRegistrationRequestDTO);
        userRegistrationJpaRepository.save(appUserEntity);
    }
}
