package com.penrose.bibby.identity.core;

import com.penrose.bibby.identity.infrastructure.AppUserImpl;
import com.penrose.bibby.identity.infrastructure.entity.AppUserEntity;
import com.penrose.bibby.identity.infrastructure.repository.UserRegistrationJpaRepository;
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
