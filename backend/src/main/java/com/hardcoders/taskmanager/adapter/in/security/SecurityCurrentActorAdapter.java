package com.hardcoders.taskmanager.adapter.in.security;

import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resuelve el actor a partir del JWT validado. Rol y estado se leen de {@code app_user} en cada
 * petición: quitar {@code is_chief} o desactivar a un usuario tiene efecto sin esperar a que expire el token.
 */
@Component
class SecurityCurrentActorAdapter implements CurrentActor {

    private final UserDirectory users;

    SecurityCurrentActorAdapter(UserDirectory users) {
        this.users = users;
    }

    @Override
    public Actor current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ForbiddenException("Operación sin usuario autenticado");
        }
        Integer userId;
        try {
            userId = Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ForbiddenException("Token sin usuario válido");
        }
        UserAccount account = users.findById(userId)
                .orElseThrow(() -> new ForbiddenException("El usuario del token ya no existe"));
        return new Actor(account.userId(), account.chief(), account.active());
    }
}
