package com.hardcoders.taskmanager.adapter.in.web.user;

import com.hardcoders.taskmanager.application.result.UserSummaryResult;

/** Respuestas HTTP de usuarios (sin hash/contraseña). */
public final class UserResponses {

    private UserResponses() {
    }

    public record UserResponse(Integer userId, String email, String givenNames, String familyNames, String fullName,
                               boolean chief, boolean active) {
        public static UserResponse from(UserSummaryResult result, String givenNames, String familyNames) {
            return new UserResponse(result.userId(), result.email(), givenNames, familyNames, result.fullName(),
                    result.chief(), result.active());
        }
    }
}
