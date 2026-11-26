package com.hardcoders.taskmanager.adapter.in.web.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contrato HTTP para creación de usuarios (EC3-01). No expone hash ni contraseña. */
public final class UserRequests {

    private UserRequests() {
    }

    public record CreateUserRequest(
            @NotBlank(message = "El correo es obligatorio")
            @Email(message = "El correo no tiene un formato válido")
            @Size(max = 254, message = "El correo no puede superar 254 caracteres")
            String email,
            @NotBlank(message = "Los nombres son obligatorios")
            @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
            String givenNames,
            @NotBlank(message = "Los apellidos son obligatorios")
            @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
            String familyNames,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 8, max = 200, message = "La contraseña debe tener entre 8 y 200 caracteres")
            String password
    ) {
    }

    /** Solicitud de actualización de perfil: solo campos permitidos, todos opcionales (actualización parcial). */
    public record UpdateUserRequest(
            @Email(message = "El correo no tiene un formato válido")
            @Size(max = 254, message = "El correo no puede superar 254 caracteres")
            String email,
            @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
            String givenNames,
            @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
            String familyNames
    ) {
    }
}
