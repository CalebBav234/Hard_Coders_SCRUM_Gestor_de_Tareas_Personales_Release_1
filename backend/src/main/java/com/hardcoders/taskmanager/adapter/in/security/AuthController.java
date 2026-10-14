package com.hardcoders.taskmanager.adapter.in.security;

import com.hardcoders.taskmanager.adapter.in.web.error.ErrorResponse;
import com.hardcoders.taskmanager.adapter.out.persistence.entity.AppUserEntity;
import com.hardcoders.taskmanager.adapter.out.persistence.repository.AppUserJpaRepository;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/** Inicio de sesión (email + contraseña BCrypt) y datos del usuario autenticado. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Hash de relleno: se compara aunque el usuario no exista, para no revelar qué correos están registrados. */
    private static final String DUMMY_HASH = "$2a$10$OjJvUsX8dauFBq4rwyH6KOV5I9qKR2IZn5fJpQf0PbsxyRc8Z0uyq";

    private final AppUserJpaRepository appUsers;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final UserDirectory users;
    private final CurrentActor currentActor;

    public AuthController(AppUserJpaRepository appUsers, PasswordEncoder passwordEncoder, JwtTokenService tokens,
                          UserDirectory users, CurrentActor currentActor) {
        this.appUsers = appUsers;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.users = users;
        this.currentActor = currentActor;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        Optional<AppUserEntity> candidate = appUsers.findByEmail(email);
        String hash = candidate.map(AppUserEntity::getPasswordHash).orElse(DUMMY_HASH);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (candidate.isEmpty() || !matches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("INVALID_CREDENTIALS", "Correo o contraseña incorrectos."));
        }
        // La contraseña ya está verificada: un usuario inactivo recibe un aviso propio en lugar
        // del mensaje genérico, para que sepa que su cuenta sigue existiendo pero está deshabilitada.
        if (!Boolean.TRUE.equals(candidate.get().getIsActive())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ErrorResponse.of("USER_INACTIVE", "Tu usuario está inactivo. Contacta al administrador."));
        }
        UserAccount account = users.findById(candidate.get().getUserId()).orElseThrow();
        JwtTokenService.IssuedToken token = tokens.issue(account);
        return ResponseEntity.ok(new LoginResponse(token.value(), token.expiresAt(), CurrentUserResponse.from(account)));
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        Actor actor = currentActor.current();
        return CurrentUserResponse.from(users.findById(actor.userId()).orElseThrow());
    }

    public record LoginRequest(
            @NotBlank(message = "El correo es obligatorio") @Size(max = 254) String email,
            @NotBlank(message = "La contraseña es obligatoria") @Size(max = 200) String password) {
    }

    public record LoginResponse(String token, Instant expiresAt, CurrentUserResponse user) {
    }

    public record CurrentUserResponse(Integer userId, String email, String givenNames, String familyNames,
                                      String fullName, boolean chief, boolean active) {
        static CurrentUserResponse from(UserAccount account) {
            return new CurrentUserResponse(account.userId(), account.email(), account.givenNames(),
                    account.familyNames(), account.fullName(), account.chief(), account.active());
        }
    }
}
