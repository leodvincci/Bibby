package com.penrose.bibby.library.registration.appuser;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class AppUserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String username;
  private String password;
  private String email;
  private String firstName;
  private String lastName;

  public AppUserEntity(
      Long id, String username, String password, String email, String firstName, String lastName) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public AppUserEntity() {}

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
