package com.hardcoders.taskmanager.adapter.in.web.user;

import com.hardcoders.taskmanager.application.command.CreateUserCommand;
import com.hardcoders.taskmanager.application.port.in.ManageUsersUseCase;
import com.hardcoders.taskmanager.application.result.UserSummaryResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** EC3-01: creación de usuarios. Solo Jefe activo (autorizado en caso de uso). */
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
        // No se aceptan campos privilegiados (is_chief/is_active): el caso de uso crea no-Jefe activo.
        String given = request.givenNames() == null ? "" : request.givenNames().strip();
        String family = request.familyNames() == null ? "" : request.familyNames().strip();
        UserSummaryResult created = manageUsers.create(new CreateUserCommand(
                request.email(), given, family, request.password()));
        return new UserResponses.UserResponse(created.userId(), created.email(), given, family,
                created.fullName(), created.chief(), created.active());
    }
}
