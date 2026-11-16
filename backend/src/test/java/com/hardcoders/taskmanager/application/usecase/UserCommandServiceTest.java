package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.PasswordHasher;
import com.hardcoders.taskmanager.domain.port.out.UserWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCommandServiceTest {

    private static final Actor CHIEF = new Actor(1, true, true);
    private static final Actor MANAGER = new Actor(2, false, true);
    private static final Actor STAFF = new Actor(3, false, true);

    private InMemoryUserWriter writer;
    private RecordingHasher hasher;
    private Actor actor;
    private UserCommandService service;

    @BeforeEach
    void setUp() {
        writer = new InMemoryUserWriter();
        hasher = new RecordingHasher();
        actor = CHIEF;
        service = new UserCommandService(writer, hasher, () -> actor);
    }

    @Test
    void chiefCreatesUserWithNormalizedEmailEncodedPasswordAndNoPrivileges() {
        UserSummaryResult created = service.create(new CreateUserCommand(
                "  NUEVO@GESTOR.LOCAL  ", "  Ana  ", "  PÃ©rez  ", "Gestor2026!"));

        assertThat(created.email()).isEqualTo("nuevo@gestor.local");
        assertThat(created.fullName()).isEqualTo("Ana PÃ©rez");
        assertThat(created.chief()).isFalse();
        assertThat(created.active()).isTrue();
        assertThat(created.userId()).isPositive();
        assertThat(writer.saved).hasSize(1);
        assertThat(writer.saved.get(0).email).isEqualTo("nuevo@gestor.local");
        assertThat(writer.saved.get(0).givenNames).isEqualTo("Ana");
        assertThat(writer.saved.get(0).familyNames).isEqualTo("PÃ©rez");
        assertThat(writer.saved.get(0).passwordHash).isEqualTo("HASHED:Gestor2026!");
        assertThat(writer.saved.get(0).passwordHash).isNotEqualTo("Gestor2026!");
    }

    @Test
    void managerAndStaffCannotCreateUsers() {
        actor = MANAGER;
        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(ForbiddenException.class);
        actor = STAFF;
        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(ForbiddenException.class);
        assertThat(writer.saved).isEmpty();
        assertThat(hasher.calls).isZero();
    }

    @Test
    void rejectsInvalidOrDuplicateEmailAndDoesNotStorePassword() {
        assertThatThrownBy(() -> service.create(new CreateUserCommand("", "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("invalido", "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b", "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);

        writer.existingEmails.add("dup@gestor.local");
        assertThatThrownBy(() -> service.create(new CreateUserCommand("DUP@gestor.local", "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("correo");

        assertThat(writer.saved).isEmpty();
    }

    @Test
    void validatesNamesAndPasswordPolicyAndLimits() {
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", " ", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "Ana", "", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "a".repeat(101), "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "Ana", "b".repeat(101), "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "Ana", "Paz", "short")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "Ana", "Paz", "x".repeat(201))))
                .isInstanceOf(DomainValidationException.class);
        assertThat(writer.saved).isEmpty();
    }

    @Test
    void handlesDuplicateRaceAsValidationErrorWithoutLeakingSensitiveData() {
        writer.throwDuplicateOnSave = true;
        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("correo")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("Gestor2026!").doesNotContain("HASHED"));
    }

    @Test
    void rejectsNullEmailTooLongEmailInvalidFormatAndBlankPassword() {
        assertThatThrownBy(() -> service.create(new CreateUserCommand(null, "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        String longLocal = "a".repeat(250) + "@b.com";
        assertThatThrownBy(() -> service.create(new CreateUserCommand(longLocal, "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("sin-arroba.com", "Ana", "Paz", "Gestor2026!")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "Ana", "Paz", "   ")))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> service.create(new CreateUserCommand("a@b.com", "Ana", "Paz", null)))
                .isInstanceOf(DomainValidationException.class);
        assertThat(writer.saved).isEmpty();
    }

    @Test
    void rethrowsUnexpectedSaveErrorsAndMapsCauseChainDuplicates() {
        writer.throwOnSave = new RuntimeException("db down");
        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("db down");

        writer.throwOnSave = new RuntimeException("wrap", new IllegalStateException("duplicate key uq_app_user_email"));
        writer.existingEmails.clear();
        writer.saved.clear();
        assertThatThrownBy(() -> service.create(validCommand()))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("correo");
    }

    private static CreateUserCommand validCommand() {
        return new CreateUserCommand("nuevo@gestor.local", "Ana", "Paz", "Gestor2026!");
    }

    private static final class InMemoryUserWriter implements UserWriter {
        final List<Saved> saved = new ArrayList<>();
        final java.util.Set<String> existingEmails = new java.util.HashSet<>();
        boolean throwDuplicateOnSave = false;
        RuntimeException throwOnSave = null;
        int nextId = 100;

        @Override
        public boolean existsByEmail(String normalizedEmail) {
            return existingEmails.contains(normalizedEmail) || saved.stream().anyMatch(s -> s.email.equals(normalizedEmail));
        }

        @Override
        public UserAccount save(String normalizedEmail, String passwordHash, String givenNames, String familyNames) {
            if (throwOnSave != null) {
                throw throwOnSave;
            }
            if (throwDuplicateOnSave || existingEmails.contains(normalizedEmail)) {
                throw new RuntimeException("duplicate key value violates unique constraint uq_app_user_email");
            }
            saved.add(new Saved(normalizedEmail, passwordHash, givenNames, familyNames));
            return new UserAccount(nextId++, normalizedEmail, givenNames, familyNames, false, true);
        }
    }

    private record Saved(String email, String passwordHash, String givenNames, String familyNames) {}

    private static final class RecordingHasher implements PasswordHasher {
        int calls;

        @Override
        public String hash(String rawPassword) {
            calls++;
            return "HASHED:" + rawPassword;
        }
    }
}
