package com.hardcoders.taskmanager.application.command;

/** Comandos de entrada de la jerarquía (US2.1 / US2.2). */
public final class HierarchyCommands {

    private HierarchyCommands() {
    }

    /** US2.1 / US2.2: crear una etapa dentro de un proyecto. */
    public record CreateStageCommand(Integer projectId, String name, String description) {
    }

    public record UpdateStageCommand(Integer stageId, String name, String description, Long version) {
    }

    public record StageStatusCommand(Integer stageId, String status, Long version) {
    }

    /** US2.2: crear una actividad dentro de una etapa. */
    public record CreateActivityCommand(Integer stageId, String name, String description) {
    }

    public record UpdateActivityCommand(Integer activityId, String name, String description, Long version) {
    }

    public record ActivityStatusCommand(Integer activityId, String status, Long version) {
    }
}
