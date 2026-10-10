package com.hardcoders.taskmanager.application.command;

/** US2.2: mover la tarea a otra actividad del mismo proyecto. */
public record ChangeActivityCommand(Integer taskId, Integer activityId, Long version) {
}
