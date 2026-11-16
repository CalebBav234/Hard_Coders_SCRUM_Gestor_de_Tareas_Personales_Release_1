package com.hardcoders.taskmanager.adapter.out.persistence;

import com.hardcoders.taskmanager.domain.port.out.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Adaptador que delega el hash al PasswordEncoder configurado por Spring Security. */
@Component
class SpringSecurityPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder;

    SpringSecurityPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
