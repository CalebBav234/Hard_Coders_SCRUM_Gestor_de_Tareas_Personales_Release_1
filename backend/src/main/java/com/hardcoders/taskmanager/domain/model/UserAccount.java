package com.hardcoders.taskmanager.domain.model;

/** Vista de dominio de {@code app_user} (sin el hash de contraseña). */
public record UserAccount(
        Integer userId,
        String email,
        String givenNames,
        String familyNames,
        boolean chief,
        boolean active
) {
    public String fullName() {
        return givenNames + " " + familyNames;
    }
}
