package com.penrose.bibby.library.registration.core;

import com.penrose.bibby.library.registration.AppUserEntity;
import com.penrose.bibby.library.registration.AppUserImpl;
import com.penrose.bibby.library.registration.UserRegistrationJpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class AppUserDetailsServiceImpl implements UserDetailsService {
    UserRegistrationJpaRepository userRegistrationJpaRepository;

    public AppUserDetailsServiceImpl(UserRegistrationJpaRepository userRegistrationJpaRepository) {
        this.userRegistrationJpaRepository = userRegistrationJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUserEntity appUser = userRegistrationJpaRepository.findByEmail(username);
        if (appUser == null) {
            throw new UsernameNotFoundException("User not found with email: " + username);
        }
        return new AppUserImpl(appUser);
    }
}
