package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.command.UpdateUserCommand;
import com.hardcoders.taskmanager.application.port.in.ManageUsersUseCase;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.policy.ProjectAccessPolicy;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.PasswordHasher;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import com.hardcoders.taskmanager.domain.port.out.UserWriter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Caso de uso: creación, actualización y desactivación de usuarios (A6/EC3-01, EC4). Solo Jefe activo. */
public class UserCommandService implements ManageUsersUseCase {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EMAIL_MAX = 254;
    private static final int NAME_MAX = 100;
    /** Límite defensivo para entrada de contraseña en claro (no se almacena). */
    private static final int PASSWORD_MAX = 200;
    /** BCrypt rechaza entradas de más de 72 bytes (UTF-8); se valida antes de hashear para responder 400. */
    private static final int PASSWORD_MAX_BYTES = 72;

    private final UserWriter users;
    private final UserDirectory directory;
    private final PasswordHasher passwordHasher;
    private final CurrentActor currentActor;

    public UserCommandService(UserWriter users, UserDirectory directory, PasswordHasher passwordHasher,
                              CurrentActor currentActor) {
        this.users = users;
        this.directory = directory;
        this.passwordHasher = passwordHasher;
        this.currentActor = currentActor;
    }

    @Override
    public UserSummaryResult create(CreateUserCommand command) {
        Actor actor = currentActor.current();
        ProjectAccessPolicy.requireChief(actor, "crear usuarios");

        String email = normalizeEmail(command.email());
        String givenNames = requireName(command.givenNames(), "nombres");
        String familyNames = requireName(command.familyNames(), "apellidos");
        String rawPassword = requirePassword(command.rawPassword());

        if (users.existsByEmail(email)) {
            throw new DomainValidationException("El correo ya está registrado");
        }

        String hash = passwordHasher.hash(rawPassword);
        try {
            UserAccount saved = users.save(email, hash, givenNames, familyNames);
            return new UserSummaryResult(saved.userId(), saved.fullName(), saved.email(), saved.chief(), saved.active());
        } catch (RuntimeException ex) {
            // Carrera de unicidad en BD: no revelar información sensible.
            if (isDuplicate(ex) || users.existsByEmail(email)) {
                throw new DomainValidationException("El correo ya está registrado");
            }
            throw ex;
        }
    }

    @Override
    public UserSummaryResult update(UpdateUserCommand command) {
        Actor actor = currentActor.current();
        ProjectAccessPolicy.requireChief(actor, "actualizar usuarios");

        Integer userId = command.userId();
        UserAccount current = directory.findById(userId)
                .orElseThrow(() -> new NotFoundException("app_user", userId));

        String email = optionalEmail(command.email());
        String givenNames = optionalName(command.givenNames(), "nombres");
        String familyNames = optionalName(command.familyNames(), "apellidos");

        if (email == null && givenNames == null && familyNames == null) {
            throw new DomainValidationException("No hay campos para actualizar");
        }

        // Cambio de email: validar formato y unicidad excluyendo al propio usuario.
        String finalEmail = current.email();
        if (email != null) {
            finalEmail = email;
            if (!finalEmail.equals(current.email()) && users.existsByEmail(finalEmail)) {
                throw new DomainValidationException("El correo ya está registrado");
            }
        }

        String finalGiven = givenNames == null ? current.givenNames() : givenNames;
        String finalFamily = familyNames == null ? current.familyNames() : familyNames;

        UserAccount updated = users.update(userId, finalEmail, finalGiven, finalFamily)
                .orElseThrow(() -> new NotFoundException("app_user", userId));
        return new UserSummaryResult(updated.userId(), updated.fullName(), updated.email(), updated.chief(), updated.active());
    }

    @Override
    public UserSummaryResult deactivate(Integer userId) {
        Actor actor = currentActor.current();
        ProjectAccessPolicy.requireChief(actor, "desactivar usuarios");

        UserAccount deactivated = users.deactivate(userId)
                .orElseThrow(() -> new NotFoundException("app_user", userId));
        return new UserSummaryResult(deactivated.userId(), deactivated.fullName(), deactivated.email(),
                deactivated.chief(), deactivated.active());
    }

    @Override
    public List<UserAccount> listUsers() {
        Actor actor = currentActor.current();
        ProjectAccessPolicy.requireChief(actor, "listar usuarios");
        return directory.findAll();
    }

    @Override
    public UserAccount getUserById(Integer userId) {
        Actor actor = currentActor.current();
        ProjectAccessPolicy.requireChief(actor, "consultar usuarios");
        return directory.findById(userId)
                .orElseThrow(() -> new NotFoundException("app_user", userId));
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            throw new DomainValidationException("El correo es obligatorio");
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new DomainValidationException("El correo es obligatorio");
        }
        if (normalized.length() > EMAIL_MAX) {
            throw new DomainValidationException("El correo no puede superar " + EMAIL_MAX + " caracteres");
        }
        if (!SIMPLE_EMAIL.matcher(normalized).matches() || !normalized.contains("@")) {
            throw new DomainValidationException("El correo no tiene un formato válido");
        }
        return normalized;
    }

    /** Email opcional en actualización: nulo/blanco → sin cambio; de lo contrario se valida formato. */
    private static String optionalEmail(String value) {
        if (value == null || value.strip().isEmpty()) {
            return null;
        }
        return normalizeEmail(value);
    }

    private static String requireName(String value, String field) {
        if (value == null || value.strip().isEmpty()) {
            throw new DomainValidationException("Los " + field + " son obligatorios");
        }
        String stripped = value.strip();
        if (stripped.length() > NAME_MAX) {
            throw new DomainValidationException("Los " + field + " no pueden superar " + NAME_MAX + " caracteres");
        }
        return stripped;
    }

    /** Nombre opcional en actualización: nulo/blanco → sin cambio; de lo contrario se valida longitud. */
    private static String optionalName(String value, String field) {
        if (value == null || value.strip().isEmpty()) {
            return null;
        }
        return requireName(value, field);
    }

    private static String requirePassword(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException("La contraseña es obligatoria");
        }
        if (value.length() > PASSWORD_MAX) {
            throw new DomainValidationException("La contraseña no puede superar " + PASSWORD_MAX + " caracteres");
        }
        if (value.length() < 8) {
            throw new DomainValidationException("La contraseña debe tener al menos 8 caracteres");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new DomainValidationException("La contraseña no puede superar " + PASSWORD_MAX_BYTES + " bytes");
        }
        return value;
    }

    private static boolean isDuplicate(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            String c = current.getClass().getName() + " " + String.valueOf(current.getMessage());
            if (c.contains("DataIntegrityViolation") || c.contains("ConstraintViolation")
                    || c.contains("constraint") || c.contains("unique") || c.contains("duplicate")
                    || c.contains("uq_app_user_email")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
