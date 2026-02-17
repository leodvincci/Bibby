package com.penrose.bibby.identity.infrastructure;

import com.penrose.bibby.identity.infrastructure.entity.AppUserEntity;
import java.util.Collection;
import java.util.Collections;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserImpl implements UserDetails {

  private final AppUserEntity appUserEntity;

  public AppUserImpl(AppUserEntity appUserEntity) {
    this.appUserEntity = appUserEntity;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singleton(new SimpleGrantedAuthority("USER"));
  }

  @Override
  public String getPassword() {
    return appUserEntity.getPassword();
  }

  @Override
  public String getUsername() {
    return appUserEntity.getEmail();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  public Long getAppUserId() {
    return appUserEntity.getId();
  }
}
