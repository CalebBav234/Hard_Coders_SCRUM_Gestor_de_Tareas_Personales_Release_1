package com.hardcoders.taskmanager.adapter.in.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del token. {@code secret} debe tener al menos 32 bytes (HS256); en producción se
 * define con la variable de entorno {@code APP_JWT_SECRET}.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, long expirationMinutes, String issuer) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.security.jwt.secret debe tener al menos 32 bytes");
        }
        if (expirationMinutes <= 0) {
            expirationMinutes = 480;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "gestor-tareas";
        }
    }
}
