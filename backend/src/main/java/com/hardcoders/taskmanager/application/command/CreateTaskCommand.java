package com.hardcoders.taskmanager.application.command;

/** @param activityName nombre de la actividad ("categoría"); nulo o vacío para "Sin categoría" */
public record CreateTaskCommand(String title, String priority, String activityName) {
}
