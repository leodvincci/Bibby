package com.penrose.bibby.WebSecurityConfigs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfigs {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/api/v1/user/registration/register").permitAll()
                    .anyRequest().authenticated()
            )
                    .csrf(csrf -> csrf.disable())
                    .formLogin(Customizer.withDefaults())
                    .logout(Customizer.withDefaults())
                .httpBasic(Customizer.withDefaults());


        return http.build();

    }

}
