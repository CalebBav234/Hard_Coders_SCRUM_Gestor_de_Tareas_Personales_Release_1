package com.hardcoders.taskmanager.application.usecase;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.command.UpdateUserCommand;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.exception.DomainValidationException;
import com.hardcoders.taskmanager.domain.exception.ForbiddenException;
import com.hardcoders.taskmanager.domain.exception.NotFoundException;
import com.hardcoders.taskmanager.domain.model.Actor;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import com.hardcoders.taskmanager.domain.port.out.CurrentActor;
import com.hardcoders.taskmanager.domain.port.out.PasswordHasher;
import com.hardcoders.taskmanager.domain.port.out.UserDirectory;
import com.hardcoders.taskmanager.domain.port.out.UserWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserCommandServiceTest {

    private static final Actor CHIEF = new Actor(1, true, true);
    private static final Actor MANAGER = new Actor(2, false, true);
    private static final Actor STAFF = new Actor(3, false, true);

    private InMemoryUserWriter writer;
    private InMemoryUserDirectory directory;
    private RecordingHasher hasher;
    private Actor actor;
    private UserCommandService service;

    @BeforeEach
    void setUp() {
        writer = new InMemoryUserWriter();
        directory = new InMemoryUserDirectory();
        hasher = new RecordingHasher();
        actor = CHIEF;
        service = new UserCommandService(writer, directory, hasher, () -> actor);
    }

    private void seed(UserAccount account) {
        directory.add(account);
        writer.existingEmails.add(account.email());
        writer.saved.add(new Saved(account.userId(), account.email(), "HASHED", account.givenNames(),
                account.familyNames(), account.chief(), account.active()));
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
    void rejectsPasswordsOverBcryptByteLimitBeforeHashing() {
        assertThat(service.create(new CreateUserCommand("p72@gestor.local", "Ana", "Paz", "x".repeat(72))).active())
                .isTrue();
        assertThatThrownBy(() -> service.create(new CreateUserCommand("p73@gestor.local", "Ana", "Paz", "x".repeat(73))))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("72 bytes");
        // 25 caracteres de 3 bytes (75 bytes) superan el límite aunque sean pocos caracteres
        assertThatThrownBy(() -> service.create(new CreateUserCommand("pmb@gestor.local", "Ana", "Paz", "\u20AC".repeat(25))))
                .isInstanceOf(DomainValidationException.class);
        assertThat(hasher.calls).isEqualTo(1);
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

    @Test
    void updateSinCamposRechazado() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        assertThatThrownBy(() -> service.update(new UpdateUserCommand(10, null, null, null)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("campos");
    }

    @Test
    void chiefUpdatesNamesAndEmail() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        UserSummaryResult updated = service.update(new UpdateUserCommand(10, "MARIA@GESTOR.LOCAL", "María", "López"));

        assertThat(updated.email()).isEqualTo("maria@gestor.local");
        assertThat(updated.fullName()).isEqualTo("María López");
        assertThat(updated.chief()).isFalse();
        assertThat(updated.active()).isTrue();
    }

    @Test
    void updatePartialFieldsOnly() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        UserSummaryResult updated = service.update(new UpdateUserCommand(10, null, "María", null));

        assertThat(updated.email()).isEqualTo("ana@gestor.local");
        assertThat(updated.fullName()).isEqualTo("María Perez");
        assertThat(updated.chief()).isFalse();
        assertThat(updated.active()).isTrue();
    }

    @Test
    void updateCannotEscalatePrivileges() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        UserSummaryResult updated = service.update(new UpdateUserCommand(10, "maria2@gestor.local", "María", "López"));

        assertThat(updated.chief()).isFalse();
        assertThat(updated.active()).isTrue();
        assertThat(updated.email()).isEqualTo("maria2@gestor.local");
    }

    @Test
    void inactiveChiefCannotUpdate() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        actor = new Actor(1, true, false);
        assertThatThrownBy(() -> service.update(new UpdateUserCommand(10, null, "María", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void managerCannotUpdate() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        actor = MANAGER;
        assertThatThrownBy(() -> service.update(new UpdateUserCommand(10, null, "María", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateNonexistentUserThrowsNotFound() {
        assertThatThrownBy(() -> service.update(new UpdateUserCommand(999, null, "María", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void duplicateEmailOnUpdateRejected() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        seed(new UserAccount(11, "luis@gestor.local", "Luis", "Gomez", false, true));
        assertThatThrownBy(() -> service.update(new UpdateUserCommand(10, "LUIS@gestor.local", "María", null)))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("correo");
    }

    @Test
    void deactivateSetsInactivePreservingIdentity() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        UserSummaryResult deactivated = service.deactivate(10);

        assertThat(deactivated.active()).isFalse();
        assertThat(deactivated.chief()).isFalse();
        assertThat(deactivated.email()).isEqualTo("ana@gestor.local");
        assertThat(deactivated.fullName()).isEqualTo("Ana Perez");
    }

    @Test
    void inactiveChiefCannotDeactivate() {
        seed(new UserAccount(10, "ana@gestor.local", "Ana", "Perez", false, true));
        actor = new Actor(1, true, false);
        assertThatThrownBy(() -> service.deactivate(10)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivateNonexistentThrowsNotFound() {
        assertThatThrownBy(() -> service.deactivate(999)).isInstanceOf(NotFoundException.class);
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
            Integer id = nextId++;
            saved.add(new Saved(id, normalizedEmail, passwordHash, givenNames, familyNames, false, true));
            return new UserAccount(id, normalizedEmail, givenNames, familyNames, false, true);
        }

        @Override
        public Optional<UserAccount> update(Integer userId, String email, String givenNames, String familyNames) {
            return saved.stream().filter(s -> s.userId().equals(userId)).findFirst().map(s ->
                    new UserAccount(userId, email, givenNames, familyNames, s.chief(), s.active()));
        }

        @Override
        public Optional<UserAccount> deactivate(Integer userId) {
            return saved.stream().filter(s -> s.userId().equals(userId)).findFirst().map(s ->
                    new UserAccount(userId, s.email(), s.givenNames(), s.familyNames(), s.chief(), false));
        }
    }

    private static final class InMemoryUserDirectory implements UserDirectory {
        final Map<Integer, UserAccount> accounts = new java.util.LinkedHashMap<>();

        @Override
        public Optional<UserAccount> findById(Integer userId) {
            return Optional.ofNullable(accounts.get(userId));
        }

        @Override
        public Map<Integer, UserAccount> findByIds(java.util.Collection<Integer> userIds) {
            return userIds.stream().filter(accounts::containsKey).distinct()
                    .collect(Collectors.toMap(Function.identity(), accounts::get));
        }

        @Override
        public List<UserAccount> findActive() {
            return accounts.values().stream().filter(UserAccount::active).toList();
        }

        @Override
        public List<UserAccount> findAll() {
            return new ArrayList<>(accounts.values());
        }

        void add(UserAccount account) {
            accounts.put(account.userId(), account);
        }
    }

    private record Saved(Integer userId, String email, String passwordHash, String givenNames, String familyNames,
                         boolean chief, boolean active) {
    }

    private static final class RecordingHasher implements PasswordHasher {
        int calls;

        @Override
        public String hash(String rawPassword) {
            calls++;
            return "HASHED:" + rawPassword;
        }
    }
}
