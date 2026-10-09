package com.hardcoders.taskmanager.application.command;

/** Comando que solo identifica el registro y la versión que el cliente vio. */
public record VersionedCommand(Integer id, Long version) {
}
