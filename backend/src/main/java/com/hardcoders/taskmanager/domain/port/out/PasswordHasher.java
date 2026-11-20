package com.hardcoders.taskmanager.domain.port.out;

/** Puerto para codificar contraseñas sin acoplar el dominio/aplicación a un framework. */
public interface PasswordHasher {

    String hash(String rawPassword);
}
