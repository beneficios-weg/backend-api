package com.weg.benefits.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class PasswordEncodingConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return newPasswordEncoder();
    }

    // The maintenance CLI must use the exact same algorithm and parameters as HTTP authentication.
    public static PasswordEncoder newPasswordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
