package com.hardcoders.taskmanager.application.command;

/** Comandos de asignación de Personal (US2.3). */
public final class AssignmentCommands {

    private AssignmentCommands() {
    }

    public record AssignWorkerCommand(Integer taskId, Integer workerUserId, String reason) {
    }

    public record UnassignWorkerCommand(Integer taskId, Integer workerUserId, Long version) {
    }
}
