package com.hardcoders.taskmanager.adapter.in.web.user;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.command.UpdateUserCommand;
import com.hardcoders.taskmanager.application.port.in.ManageUsersUseCase;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import com.hardcoders.taskmanager.domain.model.UserAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/** EC3-01 / EC4: gestión de usuarios. Solo Jefe activo (autorizado en cada caso de uso). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ManageUsersUseCase manageUsers;

    public UserController(ManageUsersUseCase manageUsers) {
        this.manageUsers = manageUsers;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponses.UserResponse create(@Valid @RequestBody UserRequests.CreateUserRequest request) {
        // Los campos de perfil son @NotBlank, por tanto nunca nulos aqu\u00ed; el caso de uso crea no-Jefe activo.
        String given = request.givenNames().strip();
        String family = request.familyNames().strip();
        UserSummaryResult created = manageUsers.create(new CreateUserCommand(
                request.email(), given, family, request.password()));
        return new UserResponses.UserResponse(created.userId(), created.email(), given, family,
                created.fullName(), created.chief(), created.active());
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<UserResponses.UserResponse> list() {
        return manageUsers.listUsers().stream()
                .map(UserController::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponses.UserResponse get(@PathVariable Integer userId) {
        UserAccount account = manageUsers.getUserById(userId);
        return toResponse(account);
    }

    @PutMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponses.UserResponse update(@PathVariable Integer userId,
                                             @Valid @RequestBody UserRequests.UpdateUserRequest request) {
        manageUsers.update(new UpdateUserCommand(
                userId, request.email(), request.givenNames(), request.familyNames()));
        // Lectura fresca para reflejar exactamente lo persistido (incluye actualizaciones parciales).
        UserAccount account = manageUsers.getUserById(userId);
        return toResponse(account);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserResponses.UserResponse deactivate(@PathVariable Integer userId) {
        manageUsers.deactivate(userId);
        // Lectura fresca para confirmar el estado de desactivación persistido.
        UserAccount account = manageUsers.getUserById(userId);
        return toResponse(account);
    }

    private static UserResponses.UserResponse toResponse(UserAccount account) {
        return new UserResponses.UserResponse(account.userId(), account.email(), account.givenNames(),
                account.familyNames(), account.fullName(), account.chief(), account.active());
    }
}
